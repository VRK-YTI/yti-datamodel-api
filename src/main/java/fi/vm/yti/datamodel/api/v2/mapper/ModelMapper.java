package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelMapper {

    private final Logger log = LoggerFactory.getLogger(ModelMapper.class);

    private final JenaService jenaService;

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
        var preferredXmlNamespacePrefix = model.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        modelResource.addProperty(preferredXmlNamespacePrefix, modelDTO.getPrefix());

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

        var organizationsModel = jenaService.getOrganizations();
        modelDTO.getOrganizations().forEach(org -> {
            var queryRes = ResourceFactory.createResource(ModelConstants.URN_UUID + org.toString());
            var resource = organizationsModel.containsResource(queryRes);
            if(resource){
                modelResource.addProperty(DCTerms.contributor, organizationsModel.getResource(ModelConstants.URN_UUID + org.toString()));
            }
        });

        return model;
    }


    public Model mapToUpdateJenaModel(String prefix, DataModelDTO dataModelDTO){
        var updateDate = new XSDDateTime(Calendar.getInstance());
        var hasUpdated = false;

        var model = jenaService.getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

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
            var organizationsModel = jenaService.getOrganizations();
            dataModelDTO.getOrganizations().forEach(org -> {
                var queryRes = ResourceFactory.createResource(ModelConstants.URN_UUID + org.toString());
                var resource = organizationsModel.containsResource(queryRes);
                if(resource){
                    modelResource.addProperty(DCTerms.contributor, organizationsModel.getResource(ModelConstants.URN_UUID + org.toString()));
                }
            });
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
            return UUID.fromString(
                    orgUri.substring(
                            orgUri.lastIndexOf(":")+ 1));
        }).collect(Collectors.toSet());
        datamodelDTO.setOrganizations(organizations);

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
        indexModel.setType(resource.getProperty(RDF.type).getObject().equals(OWL.Ontology) ? "library" : "profile");
        indexModel.setPrefix(prefix);
        indexModel.setLabel(localizedPropertyToMap(resource, RDFS.label));
        indexModel.setComment(localizedPropertyToMap(resource, RDFS.comment));
        var contributors = new ArrayList<UUID>();
        resource.listProperties(DCTerms.contributor).forEach(contributor -> {
            var value = contributor.getObject().toString();
            var uuidString = value.substring(value.lastIndexOf(":")+1);
            contributors.add(UUID.fromString(uuidString));
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


    /**
     * Localized property to Map of (language, value)
     * @param resource Resource to get property from
     * @param property Property type
     * @return Map of (language, value)
     */
    private Map<String, String> localizedPropertyToMap(Resource resource, Property property){
        var map = new HashMap<String, String>();
        resource.listProperties(property).forEach(prop -> {
            var lang = prop.getLanguage();
            var value = prop.getString();
            map.put(lang, value);
        });
        return map;
    }

    /**
     * Convert array property to list of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return List of property values
     */
    private List<String> arrayPropertyToList(Resource resource, Property property){
        var list = new ArrayList<String>();
        resource.listProperties(property).forEach(val -> list.add(val.getObject().toString()));
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
        resource.listProperties(property).forEach(val -> list.add(val.getObject().toString()));
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
}
