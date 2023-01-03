package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ModelMapper {

    private final Logger log = LoggerFactory.getLogger(ModelMapper.class);

    private final JenaService jenaService;

    public ModelMapper (JenaService jenaService){
        this.jenaService = jenaService;
    }

    private static final Map<String, String> prefixes = Map.of(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "dcterms", "http://purl.org/dc/terms/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "dcap", "http://www.w3.org/2002/07/dcap#"
    );

    public Model mapToJenaModel(DataModelDTO modelDTO) {
        log.info("Mapping DatamodelDTO to Jena Model");
        var model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(prefixes);

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

        var contentModified = model.createProperty("http://uri.suomi.fi/datamodel/ns/iow#contentModified");
        modelResource.addProperty(contentModified, ResourceFactory.createTypedLiteral(creationDate));

        var statusModified = model.createProperty("status:modified");
        modelResource.addProperty(statusModified, ResourceFactory.createTypedLiteral(creationDate));

        var preferredXmlNamespacePrefix = model.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        modelResource.addProperty(preferredXmlNamespacePrefix, modelDTO.getPrefix());

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
            var queryRes = ResourceFactory.createResource("urn:uuid:" + org.toString());
            var resource = organizationsModel.containsResource(queryRes);
            if(resource){
                modelResource.addProperty(DCTerms.contributor, organizationsModel.getResource("urn:uuid:" + org.toString()));
            }
        });

        return model;
    }

    public IndexModel mapToIndexModel(String prefix, Model model){
        var resource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        var indexModel = new IndexModel();
        indexModel.setId(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        indexModel.setStatus(resource.getProperty(OWL.versionInfo).getString());
        indexModel.setStatusModified(resource.getProperty(model.getProperty("status:modified")).getString());
        indexModel.setModified(resource.getProperty(DCTerms.modified).getString());
        indexModel.setCreated(resource.getProperty(DCTerms.created).getString());
        indexModel.setContentModified(resource.getProperty(model.getProperty(ModelConstants.SUOMI_FI_NAMESPACE + "iow#contentModified")).getString());
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

        var documentationProperty = model.getProperty(ModelConstants.SUOMI_FI_NAMESPACE + "iow#documentation");
        indexModel.setDocumentation(localizedPropertyToMap(resource, documentationProperty));

        return indexModel;
    }


    private Map<String, String> localizedPropertyToMap(Resource resource, Property property){
        var map = new HashMap<String, String>();
        resource.listProperties(property).forEach(prop -> {
            var lang = prop.getLanguage();
            var value = prop.getString();
            map.put(lang, value);
        });
        return map;
    }

    private List<String> arrayPropertyToList(Resource resource, Property property){
        var list = new ArrayList<String>();
        resource.listProperties(property).forEach(val -> list.add(val.getObject().toString()));
        return list;
    }

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
