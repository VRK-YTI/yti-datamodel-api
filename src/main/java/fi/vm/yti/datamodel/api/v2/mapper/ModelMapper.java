package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ModelMapper {

    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final SchemesRepository schemesRepository;
    private final TerminologyService terminologyService;


    public ModelMapper (CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        SchemesRepository schemesRepository,
                        TerminologyService terminologyService){
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.schemesRepository = schemesRepository;
        this.terminologyService = terminologyService;
    }

    /**
     * Map DataModelDTO to Jena model
     * @param modelDTO Data Model DTO
     * @return Model
     */
    public Model mapToJenaModel(DataModelDTO modelDTO, ModelType modelType, YtiUser user) {
        var model = ModelFactory.createDefaultModel();
        var modelUri = DataModelURI.createModelURI(modelDTO.getPrefix()).getModelURI();

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.createResource(modelUri)
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(MapperUtils.getStatusUri(Status.DRAFT)))
                .addProperty(DCTerms.identifier, UUID.randomUUID().toString())
                .addProperty(SuomiMeta.contentModified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCAP.preferredXMLNamespacePrefix, modelDTO.getPrefix())
                .addProperty(DCAP.preferredXMLNamespace, modelUri);

        MapperUtils.addCreationMetadata(modelResource, user);

        if (modelType.equals(ModelType.PROFILE)) {
            modelResource.addProperty(RDF.type, SuomiMeta.ApplicationProfile);
        }

        modelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        MapperUtils.addOptionalStringProperty(modelResource, SuomiMeta.contact, modelDTO.getContact());
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getDocumentation(), modelResource, SuomiMeta.documentation, model);
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getLabel(), modelResource, RDFS.label, model);
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getDescription(), modelResource, RDFS.comment, model);

        var groupModel = coreRepository.getServiceCategories();
        modelDTO.getGroups().forEach(group -> {
            var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
            if (groups.hasNext()) {
                modelResource.addProperty(DCTerms.isPartOf, groups.next());
            }
        });

        addOrgsToModel(modelDTO.getOrganizations(), modelResource);

        addInternalNamespaceToDatamodel(modelDTO, modelResource);
        addExternalNamespaceToDatamodel(modelDTO, model, modelResource);

        modelDTO.getLinks().forEach(linkDTO -> addLinkToModel(model, modelResource, linkDTO));

        modelDTO.getTerminologies().forEach(terminology -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, terminology));

        if(modelType.equals(ModelType.PROFILE)) {
            modelDTO.getCodeLists().forEach(codeList -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, codeList));
        }

        MapperUtils.addCreationMetadata(modelResource, user);

        return model;
    }


    public void mapToUpdateJenaModel(String graphUri, DataModelDTO dataModelDTO, Model model, YtiUser user){
        var modelResource = model.getResource(graphUri);

        //update languages before getting and using the languages for localized properties
        modelResource.removeAll(DCTerms.language);
        dataModelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        removeFromIterator(modelResource.listProperties(DCTerms.requires), val -> val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
        removeFromIterator(modelResource.listProperties(OWL.imports), val -> val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
        addInternalNamespaceToDatamodel(dataModelDTO, modelResource);

        removeFromIterator(modelResource.listProperties(DCTerms.requires), val -> !val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)
                && !val.startsWith(ModelConstants.CODELIST_NAMESPACE) && !val.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE));
        removeFromIterator(modelResource.listProperties(OWL.imports), val -> !val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)
                && !val.startsWith(ModelConstants.CODELIST_NAMESPACE) && !val.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE));
        addExternalNamespaceToDatamodel(dataModelDTO, model, modelResource);


        removeFromIterator(modelResource.listProperties(DCTerms.requires), val ->
                val.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE));
        dataModelDTO.getTerminologies().forEach(terminology -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, terminology));

        if(MapperUtils.isApplicationProfile(modelResource)){
            removeFromIterator(modelResource.listProperties(DCTerms.requires), val -> val.startsWith(ModelConstants.CODELIST_NAMESPACE));
            dataModelDTO.getCodeLists().forEach(codeList -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, codeList));
        }

        updateCommonMetaData(model, modelResource, dataModelDTO, user);
    }


    private void removeFromIterator(StmtIterator statements, Predicate<String> predicate){
        while(statements.hasNext()){
            var next = statements.next();
            var value = next.getObject().toString();
            if(predicate.test(value)){
                statements.remove();
            }
        }
    }

    private void addLinkToModel(Model model, Resource modelResource, LinkDTO linkDTO) {
        var blankNode = model.createResource();
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(languages, linkDTO.getName(), blankNode, DCTerms.title, model);
        MapperUtils.addLocalizedProperty(languages, linkDTO.getDescription(), blankNode, DCTerms.description, model);
        blankNode.addProperty(FOAF.homepage, linkDTO.getUri());
        modelResource.addProperty(RDFS.seeAlso, blankNode);
    }

    /**
     * Map a Model to DataModelExpandDTO
     *
     * @param prefix model prefix
     * @param model  Model
     * @return Data Model DTO
     */
    public DataModelInfoDTO mapToDataModelDTO(String prefix, Model model, Consumer<ResourceCommonDTO> userMapper) {

        var uri = DataModelURI.createModelURI(prefix);
        var datamodelDTO = new DataModelInfoDTO();
        datamodelDTO.setPrefix(prefix);
        datamodelDTO.setUri(uri.getGraphURI());
        var modelResource = model.getResource(uri.getModelURI());

        datamodelDTO.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(modelResource, SuomiMeta.publicationStatus)));
        datamodelDTO.setVersion(MapperUtils.propertyToString(modelResource, OWL.versionInfo));
        datamodelDTO.setVersionIri(MapperUtils.propertyToString(modelResource, OWL2.versionIRI));
        if(MapperUtils.isApplicationProfile(modelResource)){
            datamodelDTO.setType(ModelType.PROFILE);
        }else if(MapperUtils.isLibrary(modelResource)){
            datamodelDTO.setType(ModelType.LIBRARY);
        }else{
            throw new MappingError("RDF:type not supported for data model");
        }

        //Language
        datamodelDTO.setLanguages(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language));
        datamodelDTO.setLabel(MapperUtils.localizedPropertyToMap(modelResource, RDFS.label));
        datamodelDTO.setDescription(MapperUtils.localizedPropertyToMap(modelResource, RDFS.comment));

        var groups = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.isPartOf);
        datamodelDTO.setGroups(ServiceCategoryMapper.mapServiceCategoriesToDTO(groups, coreRepository.getServiceCategories()));

        var organizations = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor);
        datamodelDTO.setOrganizations(OrganizationMapper.mapOrganizationsToDTO(organizations, coreRepository.getOrganizations()));

        MapperUtils.mapCreationInfo(datamodelDTO, modelResource, userMapper);

        var internalNamespaces = new HashSet<InternalNamespaceDTO>();
        var externalNamespaces = new HashSet<ExternalNamespaceDTO>();
        addNamespacesToList(internalNamespaces, externalNamespaces, model, modelResource, OWL.imports);
        addNamespacesToList(internalNamespaces, externalNamespaces, model, modelResource, DCTerms.requires);
        datamodelDTO.setInternalNamespaces(internalNamespaces);
        datamodelDTO.setExternalNamespaces(externalNamespaces);

        var terminologies = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires)
                .stream().filter(val -> val.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE))
                .map(ref -> {
                    var terminologyModel = terminologyService.getTerminology(ref);
                    if (terminologyModel == null) {
                        return new TerminologyDTO(ref);
                    }
                    return TerminologyMapper.mapToTerminologyDTO(ref, terminologyModel);
                }).collect(Collectors.toSet());
        datamodelDTO.setTerminologies(terminologies);

        var codeLists = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires)
                .stream().filter(val -> val.startsWith(ModelConstants.CODELIST_NAMESPACE))
                .map(codeList -> {
                    var codeListModel = schemesRepository.fetch(codeList);
                    if(codeListModel == null){
                        var codeListDTO = new CodeListDTO();
                        codeListDTO.setId(codeList);
                        return codeListDTO;
                    }
                    return CodeListMapper.mapToCodeListDTO(codeList, codeListModel);
                }).collect(Collectors.toSet());
        datamodelDTO.setCodeLists(codeLists);

        if (userMapper != null) {
            userMapper.accept(datamodelDTO);
        }

        datamodelDTO.setContact(MapperUtils.propertyToString(modelResource, SuomiMeta.contact));
        datamodelDTO.setDocumentation(MapperUtils.localizedPropertyToMap(modelResource, SuomiMeta.documentation));

        var links = modelResource.listProperties(RDFS.seeAlso).toSet().stream().map(statement -> {
            var linkRes = statement.getResource();
            var linkDTO = new LinkDTO();
            linkDTO.setName(MapperUtils.localizedPropertyToMap(linkRes, DCTerms.title));
            linkDTO.setDescription(MapperUtils.localizedPropertyToMap(linkRes, DCTerms.description));
            linkDTO.setUri(MapperUtils.propertyToString(linkRes, FOAF.homepage));
            return linkDTO;
        }).collect(Collectors.toSet());
        datamodelDTO.setLinks(links);

        return datamodelDTO;
    }

    /**
     * Map a DataModel to a DataModelDocument
     * @param graphUri Graph URI
     * @param model Model
     * @return Index model
     */
    public IndexModel mapToIndexModel(String graphUri, Model model){
        var resource = model.getResource(graphUri);
        var indexModel = new IndexModel();

        var versionIri = MapperUtils.propertyToString(resource, OWL2.versionIRI);
        if(versionIri != null){
            indexModel.setId(versionIri);
        }else{
            indexModel.setId(resource.getURI());
        }
        indexModel.setUri(resource.getURI());
        indexModel.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        indexModel.setVersion(MapperUtils.propertyToString(resource, OWL.versionInfo));
        indexModel.setVersionIri(versionIri);
        indexModel.setModified(resource.getProperty(DCTerms.modified).getString());
        indexModel.setCreated(resource.getProperty(DCTerms.created).getString());
        var contentModified = resource.getProperty(SuomiMeta.contentModified);
        if(contentModified != null) {
            indexModel.setContentModified(contentModified.getString());
        }

        if(MapperUtils.isApplicationProfile(resource)){
            indexModel.setType(ModelType.PROFILE);
        }else if(MapperUtils.isLibrary(resource)){
            indexModel.setType(ModelType.LIBRARY);
        }else{
            throw new MappingError("RDF:type not supported for data model");
        }

        indexModel.setPrefix(MapperUtils.propertyToString(resource, DCAP.preferredXMLNamespacePrefix));
        indexModel.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        indexModel.setComment(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
        var contributors = MapperUtils.arrayPropertyToList(resource, DCTerms.contributor)
                .stream().map(MapperUtils::getUUID).toList();
        indexModel.setContributor(contributors);
        var isPartOf = MapperUtils.arrayPropertyToList(resource, DCTerms.isPartOf);
        var serviceCategories = coreRepository.getServiceCategories();
        var groups = isPartOf.stream().map(serviceCat -> MapperUtils.propertyToString(serviceCategories.getResource(serviceCat), SKOS.notation)).toList();
        indexModel.setIsPartOf(groups);
        indexModel.setLanguage(MapperUtils.arrayPropertyToList(resource, DCTerms.language));

        return indexModel;
    }

    /**
     * Add organizations to a model
     * @param organizations Organizations
     * @param modelResource Model resource to add orgs to
     */
    private void addOrgsToModel(Set<UUID> organizations, Resource modelResource) {
        var organizationsModel = coreRepository.getOrganizations();
        organizations.forEach(org -> {
            var orgUri = ModelConstants.URN_UUID + org;
            var orgResource = organizationsModel.getResource(orgUri);
            if (organizationsModel.containsResource(orgResource)) {
                modelResource.addProperty(DCTerms.contributor, orgResource);

                var parent = MapperUtils.propertyToString(orgResource, SuomiMeta.parentOrganization);

                if (parent != null && !parent.isEmpty() && !organizations.contains(MapperUtils.getUUID(parent))) {
                    modelResource.addProperty(DCTerms.contributor, ResourceFactory.createResource(parent));
                }
            }
        });
    }

    /**
     * Add namespaces from model to two sets
     * @param intNs Internal namespaces set - Defined by having {@link ModelConstants#SUOMI_FI_NAMESPACE} as the namespace
     * @param extNs External namespaces set - Everything not internal
     * @param model Model to get external namespace information from
     * @param resource Model resource where the given property lies
     * @param property Property to get namespace reference from
     */
    private void addNamespacesToList(Set<InternalNamespaceDTO> intNs, Set<ExternalNamespaceDTO> extNs, Model model, Resource resource, Property property){
        resource.listProperties(property)
                .filterDrop(prop -> {
                    var string = prop.getObject().toString();
                    return string.startsWith(ModelConstants.CODELIST_NAMESPACE) || string.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE);
                })
                .forEach(prop -> {
            var ns = prop.getObject().toString();
            if(ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                var dto = new InternalNamespaceDTO();
                try {
                    var nsModel = coreRepository.fetch(ns);
                    var nsResource = MapperUtils.getModelResourceFromVersion(nsModel);
                    dto.setNamespace(ns);
                    dto.setPrefix(MapperUtils.propertyToString(nsResource, DCAP.preferredXMLNamespacePrefix));
                    dto.setName(MapperUtils.localizedPropertyToMap(nsResource, RDFS.label));
                } catch (Exception e) {
                    dto.setNamespace(ns);
                }
                intNs.add(dto);
            }else {
                var extNsModel = model.getResource(ns);
                var extNsDTO = new ExternalNamespaceDTO();
                extNsDTO.setNamespace(ns);
                extNsDTO.setPrefix(MapperUtils.propertyToString(extNsModel, DCAP.preferredXMLNamespacePrefix));
                extNsDTO.setName(MapperUtils.localizedPropertyToMap(extNsModel, RDFS.label));
                extNs.add(extNsDTO);
            }
        });
    }

    /**
     * Add internal namespace to data model, if model type cannot be resolved the namespace won't be added
     * @param modelDTO Model DTO to get internal namespaces from
     * @param resource Data model resource to add linking property (OWL.imports or DCTerms.requires)
     */
    private void addInternalNamespaceToDatamodel(DataModelDTO modelDTO, Resource resource) {
        modelDTO.getInternalNamespaces().forEach(namespace -> {
            var ns = coreRepository.fetch(namespace);
            var nsRes = MapperUtils.getModelResourceFromVersion(ns);
            var nsType = MapperUtils.getModelTypeFromResource(nsRes);
            var modelType = MapperUtils.getModelTypeFromResource(resource);
            resource.addProperty(getNamespacePropertyFromType(modelType, nsType), ResourceFactory.createResource(namespace));
        });
    }

    private Property getNamespacePropertyFromType(ModelType modelType, ModelType nsType){
        if(modelType.equals(ModelType.LIBRARY) && nsType.equals(ModelType.LIBRARY)){
            return OWL.imports;
        }else if(modelType.equals(ModelType.PROFILE) && nsType.equals(ModelType.LIBRARY)){
            return DCTerms.requires;
        }else if(modelType.equals(ModelType.PROFILE) && nsType.equals(ModelType.PROFILE)){
            return OWL.imports;
        }else{
            return DCTerms.requires;
        }
    }

    /**
     * Add external namespaces to data model
     * @param modelDTO Model DTO to get external namespaces from
     * @param model Data model to add namespace resource to
     * @param resource Data model resource to add linking property (OWL.imports or DCTerms.requires)
     */
    private void addExternalNamespaceToDatamodel(DataModelDTO modelDTO, Model model, Resource resource){
        // remove existing external namespaces before adding new ones
        var oldNamespaces = model.listSubjectsWithProperty(DCAP.preferredXMLNamespacePrefix)
                .filterDrop(s -> s.getURI().startsWith(ModelConstants.SUOMI_FI_NAMESPACE))
                .toList();
        oldNamespaces.forEach(ns -> model.removeAll(ns, null, null));

        modelDTO.getExternalNamespaces().forEach(namespace -> {
            var nsUri = namespace.getNamespace();
            var nsRes = model.createResource(nsUri);

            MapperUtils.updateLocalizedProperty(modelDTO.getLanguages(), namespace.getName(), nsRes, RDFS.label, model);
            nsRes.addProperty(DCAP.preferredXMLNamespacePrefix, namespace.getPrefix());

            try{
                var resolvedNs = importsRepository.fetch(nsUri);
                var extRes = resolvedNs.getResource(nsUri);
                var nsType = MapperUtils.getModelTypeFromResource(extRes);
                var modelType = MapperUtils.getModelTypeFromResource(resource);
                resource.addProperty(getNamespacePropertyFromType(modelType, nsType), nsRes);
            }catch(Exception e){
                //If namespace wasn't resolved just add it as dcterms:requires
                resource.addProperty(DCTerms.requires, nsRes);
            }
        });
    }

    public void mapReleaseProperties(Model model, DataModelURI uri, Status status){
        var res = model.getResource(uri.getModelURI());
        res.addProperty(OWL2.versionIRI, ResourceFactory.createResource(uri.getGraphURI()));
        res.addProperty(OWL.versionInfo, uri.getVersion());
        model.listSubjectsWithProperty(SuomiMeta.publicationStatus).forEach(subject -> MapperUtils.updateUriProperty(subject, SuomiMeta.publicationStatus, MapperUtils.getStatusUri(status)));
    }

    public void mapPriorVersion(Model model, String modelUri, String priorVersionUri) {
        var resource = model.getResource(modelUri);
        resource.removeAll(OWL.priorVersion);
        resource.addProperty(OWL.priorVersion, ResourceFactory.createResource(priorVersionUri));
    }

    public ModelVersionInfo mapModelVersionInfo(Resource resource) {
        var dto = new ModelVersionInfo();
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setVersionIRI(MapperUtils.propertyToString(resource, OWL2.versionIRI));
        dto.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        dto.setVersion(MapperUtils.propertyToString(resource, OWL.versionInfo));
        return dto;
    }

    public void updateCommonMetaData(Model model, Resource resource, ModelMetaData dto, YtiUser user) {
        var langs = MapperUtils.arrayPropertyToSet(resource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(langs, dto.getLabel(), resource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(langs, dto.getDescription(), resource, RDFS.comment, model);
        MapperUtils.updateStringProperty(resource, SuomiMeta.contact, dto.getContact());
        MapperUtils.updateLocalizedProperty(langs, dto.getDocumentation(), resource, SuomiMeta.documentation, model);

        resource.removeAll(DCTerms.contributor);
        addOrgsToModel(dto.getOrganizations(), resource);

        resource.removeAll(DCTerms.isPartOf);
        var groupModel = coreRepository.getServiceCategories();
        dto.getGroups().forEach(group -> {
            var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
            if (groups.hasNext()) {
                resource.addProperty(DCTerms.isPartOf, groups.next());
            }
        });

        // remove blank nodes
        resource.listProperties(RDFS.seeAlso)
                .mapWith(Statement::getObject)
                .forEach(obj -> model.removeAll(obj.asResource(), null, null));
        resource.removeAll(RDFS.seeAlso);
        dto.getLinks().forEach(linkDTO -> addLinkToModel(model, resource, linkDTO));

        MapperUtils.addUpdateMetadata(resource, user);
    }

    public void mapUpdateVersionedModel(Model model, String modelUri, VersionedModelDTO dto, YtiUser user) {
        var resource = model.getResource(modelUri);

        var status = MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus));
        if(status.equals(Status.VALID) && !List.of(Status.VALID, Status.RETIRED, Status.SUPERSEDED).contains(dto.getStatus())){
            throw new MappingError("Cannot change status from VALID to " + dto.getStatus());
        }else if(status.equals(Status.SUGGESTED) && !List.of(Status.SUGGESTED, Status.VALID, Status.RETIRED, Status.SUPERSEDED).contains(dto.getStatus())) {
            throw new MappingError("Cannot change status from SUGGESTED to " + dto.getStatus());
        }

        model.listSubjectsWithProperty(SuomiMeta.publicationStatus).forEach(subject -> MapperUtils.updateUriProperty(subject, SuomiMeta.publicationStatus, dto.getStatus().name()));

        updateCommonMetaData(model, resource, dto, user);
    }
}
