package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.datamodel.api.v2.mapper.MapperUtils.localizedPropertyToMap;
import static fi.vm.yti.datamodel.api.v2.mapper.MapperUtils.getUUID;

@Service
public class ModelMapper {

    private final Logger log = LoggerFactory.getLogger(ModelMapper.class);

    private final JenaService jenaService;

    private static final Property PREFERRED_XML_NAMESPACE_PREFIX = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");

    public ModelMapper (JenaService jenaService){
        this.jenaService = jenaService;
    }

    /**
     * Map DataModelDTO to Jena model
     * @param modelDTO Data Model DTO
     * @return Model
     */
    public Model mapToJenaModel(DataModelDTO modelDTO) {
        log.info("Mapping DatamodelDTO to Jena Model");
        var model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(ModelConstants.PREFIXES);

        // TODO: type of application profile?
        Resource type = modelDTO.getType().equals(ModelType.LIBRARY)
                ? OWL.Ontology
                : ResourceFactory.createProperty("http://www.w3.org/2002/07/dcap#DCAP");

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.createResource(ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix())
                .addProperty(RDF.type, type)
                .addProperty(OWL.versionInfo, modelDTO.getStatus().name())
                .addProperty(DCTerms.identifier, UUID.randomUUID().toString())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate));

        modelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        modelResource.addProperty(Iow.contentModified, ResourceFactory.createTypedLiteral(creationDate));


        //TODO is this needed
        modelResource.addProperty(PREFERRED_XML_NAMESPACE_PREFIX, modelDTO.getPrefix());

        //TODO is this needed
        var preferredXmlNamespace = model.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
        modelResource.addProperty(preferredXmlNamespace, ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix());

        addLocalizedProperty(modelDTO.getLabel(), modelResource, RDFS.label, model);
        addLocalizedProperty(modelDTO.getDescription(), modelResource, RDFS.comment, model);

        var groupModel = jenaService.getServiceCategories();
        modelDTO.getGroups().forEach(group -> {
            var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
            if (groups.hasNext()) {
                modelResource.addProperty(DCTerms.isPartOf, groups.next());
            }
        });

        addOrgsToModel(modelDTO, modelResource);

        addInternalNamespaceToDatamodel(modelDTO, modelResource);
        //As this could not be reasonably checked using the constraint validator we do not add external namespaces to core vocabularies
        if(!modelDTO.getType().equals(ModelType.LIBRARY)){
            addExternalNamespaceToDatamodel(modelDTO, model, modelResource);
        }

        return model;
    }


    public Model mapToUpdateJenaModel(String prefix, DataModelDTO dataModelDTO, Model model){
        var updateDate = new XSDDateTime(Calendar.getInstance());
        var hasUpdated = false;

        var modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        if(dataModelDTO.getStatus() != null){
            modelResource.removeAll(OWL.versionInfo);
            modelResource.addProperty(OWL.versionInfo, dataModelDTO.getStatus().name());
            hasUpdated = true;
        }

        if(dataModelDTO.getLabel() != null){
            modelResource.removeAll(RDFS.label);
            addLocalizedProperty(dataModelDTO.getLabel(), modelResource, RDFS.label, model);
            hasUpdated = true;
        }

        if(dataModelDTO.getDescription() != null){
            modelResource.removeAll(RDFS.comment);
            addLocalizedProperty(dataModelDTO.getDescription(), modelResource, RDFS.comment, model);
            hasUpdated = true;
        }

        if(dataModelDTO.getLanguages() != null){
            modelResource.removeAll(DCTerms.language);
            dataModelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));
            hasUpdated = true;
        }

        if(dataModelDTO.getGroups() != null){
            modelResource.removeAll(DCTerms.isPartOf);
            var groupModel = jenaService.getServiceCategories();
            dataModelDTO.getGroups().forEach(group -> {
                var groups = groupModel.listResourcesWithProperty(SKOS.notation, group);
                if (groups.hasNext()) {
                    modelResource.addProperty(DCTerms.isPartOf, groups.next());
                }
            });
            hasUpdated = true;
        }

        if(dataModelDTO.getOrganizations() != null){
            modelResource.removeAll(DCTerms.contributor);
            addOrgsToModel(dataModelDTO, modelResource);
            hasUpdated = true;
        }


        if(dataModelDTO.getInternalNamespaces() != null){
            var reqIterator = modelResource.listProperties(DCTerms.requires);
            while(reqIterator.hasNext()){
                var next = reqIterator.next();
                if(next.getObject().toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                    reqIterator.remove();
                }
            }
            var importsIterator = modelResource.listProperties(OWL.imports);
            while(importsIterator.hasNext()){
                var next = importsIterator.next();
                if(next.getObject().toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                    importsIterator.remove();
                }
            }
            addInternalNamespaceToDatamodel(dataModelDTO, modelResource);
            hasUpdated = true;
        }


        //As this could not be reasonably checked using the constraint validator we do not add external namespaces to core vocabularies(Ontologies)
        if(dataModelDTO.getExternalNamespaces() != null && !modelResource.getProperty(RDF.type).getResource().equals(OWL.Ontology)){
            var reqIterator = modelResource.listProperties(DCTerms.requires);
            while(reqIterator.hasNext()){
                var next = reqIterator.next();
                if(!next.getObject().toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                    reqIterator.remove();
                }
            }
            var importsIterator = modelResource.listProperties(OWL.imports);
            while(importsIterator.hasNext()){
                var next = importsIterator.next();
                if(!next.getObject().toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                    importsIterator.remove();
                }
            }

            addExternalNamespaceToDatamodel(dataModelDTO, model, modelResource);
            hasUpdated = true;
        }

        if(hasUpdated){
            modelResource.removeAll(DCTerms.modified);
            modelResource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(updateDate));
        }

        return model;
    }

    /**
     * Map a Model to DataModelDTO
     * @param prefix model prefix
     * @param model Model
     * @return Data Model DTO
     */
    public DataModelDTO mapToDataModelDTO(String prefix, Model model) {

        var datamodelDTO = new DataModelDTO();
        datamodelDTO.setPrefix(prefix);

        var modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        var type = modelResource.getProperty(RDF.type).getObject().equals(OWL.Ontology) ? ModelType.LIBRARY : ModelType.PROFILE;
        datamodelDTO.setType(type);

        var status = Status.valueOf(modelResource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        datamodelDTO.setStatus(status);

        //Language
        datamodelDTO.setLanguages(arrayPropertyToSet(modelResource, DCTerms.language));

        //Label
        datamodelDTO.setLabel(localizedPropertyToMap(modelResource, RDFS.label));

        //Description
        datamodelDTO.setDescription(localizedPropertyToMap(modelResource, RDFS.comment));

        var existingGroups = jenaService.getServiceCategories();
        var groups = modelResource.listProperties(DCTerms.isPartOf).toList().stream().map(prop -> {
            var resource = existingGroups.getResource(prop.getObject().toString());
            return resource.getProperty(SKOS.notation).getObject().toString();
        }).collect(Collectors.toSet());
        datamodelDTO.setGroups(groups);

        var organizations = modelResource.listProperties(DCTerms.contributor).toList().stream().map(prop -> {
            var orgUri = prop.getObject().toString();
            return getUUID(orgUri);
        }).collect(Collectors.toSet());
        datamodelDTO.setOrganizations(organizations);

        var internalNamespaces = new HashSet<String>();
        var externalNamespaces = new HashSet<ExternalNamespaceDTO>();
        addNamespacesToList(internalNamespaces, externalNamespaces, model, modelResource, OWL.imports);
        addNamespacesToList(internalNamespaces, externalNamespaces, model, modelResource, DCTerms.requires);
        datamodelDTO.setInternalNamespaces(internalNamespaces);
        datamodelDTO.setExternalNamespaces(externalNamespaces);

        return datamodelDTO;
    }

    /**
     * Map a DataModel to a IndexModel
     * @param prefix Prefix of model
     * @param model Model
     * @return Index model
     */
    public IndexModel mapToIndexModel(String prefix, Model model){
        var resource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        var indexModel = new IndexModel();
        indexModel.setId(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        indexModel.setStatus(resource.getProperty(OWL.versionInfo).getString());
        indexModel.setModified(resource.getProperty(DCTerms.modified).getString());
        indexModel.setCreated(resource.getProperty(DCTerms.created).getString());

        var contentModified = resource.getProperty(Iow.contentModified);
        if(contentModified != null){
            indexModel.setContentModified(contentModified.getString());
        }
        indexModel.setType(resource.getProperty(RDF.type).getObject().equals(OWL.Ontology)
                ? ModelType.LIBRARY.name()
                : ModelType.PROFILE.name());
        indexModel.setPrefix(prefix);
        indexModel.setLabel(localizedPropertyToMap(resource, RDFS.label));
        indexModel.setComment(localizedPropertyToMap(resource, RDFS.comment));
        var contributors = new ArrayList<UUID>();
        resource.listProperties(DCTerms.contributor).forEach(contributor -> {
            var value = contributor.getObject().toString();
            contributors.add(getUUID(value));
        });
        indexModel.setContributor(contributors);
        var isPartOf = arrayPropertyToList(resource, DCTerms.isPartOf);
        var serviceCategories = jenaService.getServiceCategories();
        var groups = isPartOf.stream().map(serviceCat -> serviceCategories.getResource(serviceCat).getProperty(SKOS.notation).getObject().toString()).collect(Collectors.toList());
        indexModel.setIsPartOf(groups);
        indexModel.setLanguage(arrayPropertyToList(resource, DCTerms.language));

        indexModel.setDocumentation(localizedPropertyToMap(resource, Iow.documentation));

        return indexModel;
    }

    public List<ServiceCategoryDTO> mapToListServiceCategoryDTO(Model serviceCategoryModel) {
        var iterator = serviceCategoryModel.listResourcesWithProperty(RDF.type, FOAF.Group);
        List<ServiceCategoryDTO> result = new ArrayList<>();

        while (iterator.hasNext()) {
            var resource = iterator.next().asResource();
            var labels = localizedPropertyToMap(resource, RDFS.label);
            var identifier = resource.getProperty(SKOS.notation).getObject().toString();
            result.add(new ServiceCategoryDTO(resource.getURI(), labels, identifier));
        }
        return result;
    }

    /**
     * Convert array property to list of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return List of property values
     */
    private List<String> arrayPropertyToList(Resource resource, Property property){
        var list = new ArrayList<String>();
        try{
            resource.getProperty(property)
                    .getList()
                    .asJavaList()
                    .forEach(node -> list.add(node.toString()));
        }catch(JenaException ex){
            //if item could not be gotten as list it means it is multiple statements of the property
            resource.listProperties(property)
                    .forEach(val -> list.add(val.getObject().toString()));
        }
        return list;
    }

    /**
     * Convert array property to set of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return Set of property values
     */
    private Set<String> arrayPropertyToSet(Resource resource, Property property){
        var list = new HashSet<String>();
        try{
            resource.getProperty(property)
                    .getList()
                    .asJavaList()
                    .forEach(node -> list.add(node.toString()));
        }catch(JenaException ex){
            //if item could not be gotten as list it means it is multiple statements of the property
            resource.listProperties(property)
                    .forEach(val -> list.add(val.getObject().toString()));
        }
        return list;
    }

    /**
     * Add localized property to model
     * @param data Map of (language, value)
     * @param resource Resource to add to
     * @param property Property to add
     * @param model Model to add to
     */
    private void addLocalizedProperty(Map<String, String> data,
                                      Resource resource,
                                      Property property,
                                      Model model) {
        if (data == null) {
            return;
        }
        data.forEach((lang, value) -> resource.addProperty(property, model.createLiteral(value, lang)));
    }


    /**
     * Add organizations to a model
     * @param modelDTO Payload to get organizations from
     * @param modelResource Model resource to add orgs to
     */
    private void addOrgsToModel(DataModelDTO modelDTO, Resource modelResource) {
        var organizationsModel = jenaService.getOrganizations();
        modelDTO.getOrganizations().forEach(org -> {
            var queryRes = ResourceFactory.createResource(ModelConstants.URN_UUID + org.toString());
            var resource = organizationsModel.containsResource(queryRes);
            if(resource){
                modelResource.addProperty(DCTerms.contributor, organizationsModel.getResource(ModelConstants.URN_UUID + org));
            }
        });
    }

    /**
     * Add namespaces from model to two sets
     * @param intNs Internal namespaces set - Defined by having http://uri.suomi.fi/ as the namespace
     * @param extNs External namespaces set - Everything not internal
     * @param model Model to get external namespace information from
     * @param resource Model resource where the given property lies
     * @param property Property to get namespace reference from
     */
    private void addNamespacesToList(Set<String> intNs, Set<ExternalNamespaceDTO> extNs, Model model, Resource resource, Property property){
        resource.listProperties(property).forEach(prop -> {
            var ns = prop.getObject().toString();
            if(ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)){
                intNs.add(ns);
            }else {
                var extNsModel = model.getResource(ns);
                var extPrefix = extNsModel.getProperty(PREFERRED_XML_NAMESPACE_PREFIX).getObject().toString();
                var extNsDTO = new ExternalNamespaceDTO();
                extNsDTO.setNamespace(ns);
                extNsDTO.setPrefix(extPrefix);
                extNsDTO.setName(extNsModel.getProperty(RDFS.label).getObject().toString());
                extNs.add(extNsDTO);
            }
        });
    }

    /**
     * Add internal namespace to data model, if model type cannot be resolved the namespace won't be added
     * @param modelDTO Model DTO to get internal namespaces from
     * @param resource Data model resource to add linking property (OWL.imports or DCTerms.requires)
     */
    private void addInternalNamespaceToDatamodel(DataModelDTO modelDTO, Resource resource){
        modelDTO.getInternalNamespaces().forEach(namespace -> {
            var ns = jenaService.getDataModel(namespace);
            if(ns != null){
                var nsType = ns.getResource(namespace).getProperty(RDF.type).getResource();
                if(nsType.equals(OWL.Ontology)){
                    resource.addProperty(OWL.imports, namespace);
                }else{
                    resource.addProperty(DCTerms.requires, namespace);
                }
            }
        });
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
                var resolvedNamespace = ModelFactory.createDefaultModel();
                try{
                    resolvedNamespace.read(nsUri);
                    var extRes = resolvedNamespace.getResource(nsUri);
                    var resType = extRes.getProperty(RDF.type).getResource();

                    var nsRes = model.createResource(nsUri);
                    nsRes.addProperty(RDFS.label, namespace.getName());
                    nsRes.addProperty(PREFERRED_XML_NAMESPACE_PREFIX, namespace.getPrefix());
                    nsRes.addProperty(DCTerms.type, resType);
                    if(resType.equals(OWL.Ontology)){
                        resource.addProperty(OWL.imports, nsUri);
                    }else{
                        resource.addProperty(DCTerms.requires, nsUri);
                    }
                }catch (HttpException ex){
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                        log.warn("Model not found with prefix {}", nsUri);
                        throw new ResourceNotFoundException(nsUri);
                    } else {
                        throw new RuntimeException("Error fetching external namespace");
                    }
                }

            });
    }
}
