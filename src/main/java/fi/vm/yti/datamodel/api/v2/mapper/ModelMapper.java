package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ModelMapper {

    private final Logger log = LoggerFactory.getLogger(ModelMapper.class);

    private final CoreRepository coreRepository;
    private final ConceptRepository conceptRepository;
    private final ImportsRepository importsRepository;
    private final SchemesRepository schemesRepository;


    public ModelMapper (CoreRepository coreRepository,
                        ConceptRepository conceptRepository,
                        ImportsRepository importsRepository,
                        SchemesRepository schemesRepository){
        this.coreRepository = coreRepository;
        this.conceptRepository = conceptRepository;
        this.importsRepository = importsRepository;
        this.schemesRepository = schemesRepository;
    }

    /**
     * Map DataModelDTO to Jena model
     * @param modelDTO Data Model DTO
     * @return Model
     */
    public Model mapToJenaModel(DataModelDTO modelDTO, ModelType modelType, YtiUser user) {
        log.info("Mapping DatamodelDTO to Jena Model");
        var model = ModelFactory.createDefaultModel();
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix();

        model.setNsPrefixes(ModelConstants.PREFIXES);

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.createResource(modelUri)
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name())
                .addProperty(DCTerms.identifier, UUID.randomUUID().toString())
                .addProperty(Iow.contentModified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCAP.preferredXMLNamespacePrefix, modelDTO.getPrefix())
                .addProperty(DCAP.preferredXMLNamespace, modelUri);

        MapperUtils.addCreationMetadata(modelResource, user);

        if (modelType.equals(ModelType.PROFILE)) {
            modelResource.addProperty(RDF.type, Iow.ApplicationProfile);
        }

        modelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        MapperUtils.addOptionalStringProperty(modelResource, Iow.contact, modelDTO.getContact());
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getDocumentation(), modelResource, Iow.documentation, model);
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getLabel(), modelResource, RDFS.label, model);
        MapperUtils.addLocalizedProperty(modelDTO.getLanguages(), modelDTO.getDescription(), modelResource, RDFS.comment, model);

        var groupModel = coreRepository.getServiceCategories();
        modelDTO.getGroups().forEach(group -> {
            var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
            if (groups.hasNext()) {
                modelResource.addProperty(DCTerms.isPartOf, groups.next());
            }
        });

        addOrgsToModel(modelDTO, modelResource);

        addInternalNamespaceToDatamodel(modelDTO, modelResource, model);
        addExternalNamespaceToDatamodel(modelDTO, model, modelResource);

        modelDTO.getLinks().forEach(linkDTO -> addLinkToModel(model, modelResource, linkDTO));

        modelDTO.getTerminologies().forEach(terminology -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, terminology));

        if(modelType.equals(ModelType.PROFILE)) {
            modelDTO.getCodeLists().forEach(codeList -> MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, codeList));
        }

        model.setNsPrefix(modelDTO.getPrefix(), modelUri + ModelConstants.RESOURCE_SEPARATOR);
        MapperUtils.addCreationMetadata(modelResource, user);

        return model;
    }


    public Model mapToUpdateJenaModel(String prefix, DataModelDTO dataModelDTO, Model model, YtiUser user){
        var modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        //update languages before getting and using the languages for localized properties
        modelResource.removeAll(DCTerms.language);
        dataModelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(langs, dataModelDTO.getLabel(), modelResource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(langs, dataModelDTO.getDescription(), modelResource, RDFS.comment, model);
        MapperUtils.updateStringProperty(modelResource, Iow.contact, dataModelDTO.getContact());
        MapperUtils.updateLocalizedProperty(langs, dataModelDTO.getDocumentation(), modelResource, Iow.documentation, model);

        modelResource.removeAll(DCTerms.isPartOf);
        var groupModel = coreRepository.getServiceCategories();
        dataModelDTO.getGroups().forEach(group -> {
            var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
            if (groups.hasNext()) {
                modelResource.addProperty(DCTerms.isPartOf, groups.next());
            }
        });

        modelResource.removeAll(DCTerms.contributor);
        addOrgsToModel(dataModelDTO, modelResource);

        removeFromIterator(modelResource.listProperties(DCTerms.requires), val -> val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
        removeFromIterator(modelResource.listProperties(OWL.imports), val -> val.startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
        addInternalNamespaceToDatamodel(dataModelDTO, modelResource, model);

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

        // remove blank nodes
        modelResource.listProperties(RDFS.seeAlso)
                .mapWith(Statement::getObject)
                .forEach(obj -> model.removeAll(obj.asResource(), null, null));
        modelResource.removeAll(RDFS.seeAlso);
        dataModelDTO.getLinks().forEach(linkDTO -> addLinkToModel(model, modelResource, linkDTO));

        MapperUtils.addUpdateMetadata(modelResource, user);
        return model;
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
        blankNode.addProperty(DCTerms.title, linkDTO.getName());
        MapperUtils.addOptionalStringProperty(blankNode, DCTerms.description, linkDTO.getDescription());
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

        var datamodelDTO = new DataModelInfoDTO();
        datamodelDTO.setPrefix(prefix);

        var modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        datamodelDTO.setStatus(Status.valueOf(MapperUtils.propertyToString(modelResource, SuomiMeta.publicationStatus)));
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
                    var terminologyModel = conceptRepository.fetch(ref);
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

        datamodelDTO.setContact(MapperUtils.propertyToString(modelResource, Iow.contact));
        datamodelDTO.setDocumentation(MapperUtils.localizedPropertyToMap(modelResource, Iow.documentation));

        var links = modelResource.listProperties(RDFS.seeAlso).toSet().stream().map(statement -> {
            var linkRes = statement.getResource();
            var linkDTO = new LinkDTO();
            linkDTO.setName(MapperUtils.propertyToString(linkRes, DCTerms.title));
            linkDTO.setDescription(MapperUtils.propertyToString(linkRes, DCTerms.description));
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
        indexModel.setStatus(Status.valueOf(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        indexModel.setVersion(MapperUtils.propertyToString(resource, OWL.versionInfo));
        indexModel.setVersionIri(versionIri);
        indexModel.setModified(resource.getProperty(DCTerms.modified).getString());
        indexModel.setCreated(resource.getProperty(DCTerms.created).getString());
        var contentModified = resource.getProperty(Iow.contentModified);
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
     * @param modelDTO Payload to get organizations from
     * @param modelResource Model resource to add orgs to
     */
    private void addOrgsToModel(DataModelDTO modelDTO, Resource modelResource) {
        var organizationsModel = coreRepository.getOrganizations();
        modelDTO.getOrganizations().forEach(org -> {
            var orgUri = ModelConstants.URN_UUID + org;
            var queryRes = ResourceFactory.createResource(orgUri);
            if(organizationsModel.containsResource(queryRes)){
                modelResource.addProperty(DCTerms.contributor, organizationsModel.getResource(orgUri));
            }
        });
    }

    /**
     * Add namespaces from model to two sets
     * @param intNs Internal namespaces set - Defined by having http://uri.suomi.fi/datamodel/ns as the namespace
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
                extNsDTO.setName(MapperUtils.propertyToString(extNsModel, RDFS.label));
                extNs.add(extNsDTO);
            }
        });
    }

    /**
     * Add internal namespace to data model, if model type cannot be resolved the namespace won't be added
     * @param modelDTO Model DTO to get internal namespaces from
     * @param resource Data model resource to add linking property (OWL.imports or DCTerms.requires)
     */
    private void addInternalNamespaceToDatamodel(DataModelDTO modelDTO, Resource resource, Model model){
        modelDTO.getInternalNamespaces().forEach(namespace -> {
            var ns = coreRepository.fetch(namespace);
            var nsRes = MapperUtils.getModelResourceFromVersion(ns);
            var prefix = MapperUtils.propertyToString(nsRes, DCAP.preferredXMLNamespacePrefix);
            var nsType = MapperUtils.getModelTypeFromResource(nsRes);
            var modelType = MapperUtils.getModelTypeFromResource(resource);
            resource.addProperty(getNamespacePropertyFromType(modelType, nsType), ResourceFactory.createResource(namespace));
            model.setNsPrefix(prefix, namespace);
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
            modelDTO.getExternalNamespaces().forEach(namespace -> {
                var nsUri = namespace.getNamespace();
                var nsRes = model.createResource(nsUri);
                nsRes.addProperty(RDFS.label, namespace.getName());
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
                model.setNsPrefix(namespace.getPrefix(), nsUri);
        });
    }

    public String mapReleaseProperties(Model model, String graphUri, String version, Status status){

        /*
        TODO: NOT MVP VC
        owl:deprecated --> boolean is deprecated (same as Status.RETIRED?)
        suomi-meta:onMuutostieto --> commit message
        owl:backwardsCompatibleWith --> some versionIri
         */

        var versionIri = graphUri + ModelConstants.RESOURCE_SEPARATOR + version;
        var res = model.getResource(graphUri);
        res.addProperty(OWL2.versionIRI, ResourceFactory.createResource(versionIri));
        res.addProperty(OWL.versionInfo, version);
        MapperUtils.updateStringProperty(res, SuomiMeta.publicationStatus, status.name());
        //prior version doesn't need to be mapped since it should always be already on the DRAFT model
        return versionIri;
    }

    public void mapPriorVersion(Model model, String graphUri, String priorVersionUri) {
        var resource = model.getResource(graphUri);
        resource.removeAll(OWL.priorVersion);
        resource.addProperty(OWL.priorVersion, ResourceFactory.createResource(priorVersionUri));
    }

    public ModelVersionInfo mapModelVersionInfo(Resource resource) {
        var dto = new ModelVersionInfo();
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setVersionIRI(MapperUtils.propertyToString(resource, OWL2.versionIRI));
        dto.setStatus(Status.valueOf(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        dto.setVersion(MapperUtils.propertyToString(resource, OWL.versionInfo));
        return dto;
    }

    public void mapUpdateVersionedModel(Model model, String graphUri, String version, VersionedModelDTO dto, YtiUser user) {
        var resource = model.getResource(graphUri);
        var status = MapperUtils.arrayPropertyToList(resource, OWL.versionInfo).stream()
                .filter(ver -> Arrays.stream(Status.values())
                        .map(Status::name)
                        .anyMatch(ver::equals))
                .map(Status::valueOf)
                .findFirst()
                .orElse(null);

        var langs = MapperUtils.arrayPropertyToSet(resource, DCTerms.language);


        if(status.equals(Status.VALID) && !List.of(Status.VALID, Status.RETIRED, Status.SUPERSEDED).contains(dto.getStatus())){
            throw new MappingError("Cannot change status from VALID to " + dto.getStatus());
        }else if(status.equals(Status.SUGGESTED) && !List.of(Status.SUGGESTED, Status.VALID, Status.RETIRED, Status.SUPERSEDED).contains(dto.getStatus())) {
            throw new MappingError("Cannot change status from SUGGESTED to " + dto.getStatus());
        }

        //TODO change this when moving status from owl:versionInfo
        MapperUtils.updateStringProperty(resource, OWL.versionInfo, dto.getStatus().name());
        resource.addProperty(OWL.versionInfo, version);
        MapperUtils.updateLocalizedProperty(langs, dto.getDocumentation(), resource, Iow.documentation, model);

        MapperUtils.addUpdateMetadata(resource, user);

    }
}
