package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.*;

import java.util.Map;

public class ModelMapper {

    Map<String, String> prefixes = Map.of(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "dcterms", "http://purl.org/dc/terms/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "dcap", "http://www.w3.org/2002/07/dcap#"
    );

    // TODO: fetch these from the database
    Map<String, String> groupMap = Map.of(
              "P11", "v1105"
    );

    public Model mapToJenaModel(DataModelDTO modelDTO) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(prefixes);

        // TODO: type of application profile?
        Resource type = modelDTO.getType().equals(ModelType.LIBRARY)
                ? OWL.Ontology
                : ResourceFactory.createProperty("http://www.w3.org/2002/07/dcap#DCAP");

        Resource modelResource = model.createResource(modelDTO.getId())
                .addProperty(RDF.type, type)
                .addProperty(OWL.versionInfo, modelDTO.getStatus().name());

        modelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        addLocalizedProperty(modelDTO.getLabel(), modelResource, RDFS.label, model);
        addLocalizedProperty(modelDTO.getDescription(), modelResource, RDFS.comment, model);

        modelDTO.getGroups().forEach(group -> {
            // TODO: fetch groups from Fuseki
            String groupId = groupMap.get(group);
            if (groupId != null) {
                model.createResource("http://urn.fi/URN:NBN:fi:au:ptvl:" + groupId)
                        .addProperty(DCTerms.identifier, group)
                        .addProperty(RDFS.label, model.createLiteral("Group todo label", "fi"));
                modelResource.addProperty(DCTerms.isPartOf, group);
            }
        });

        modelDTO.getOrganizations().forEach(org -> {
            model.createResource(String.format("urn:uuid:%s", org.toString()));
            modelResource.addProperty(DCTerms.contributor, org.toString());
        });

        RDFDataMgr.write(System.out, model, Lang.JSONLD);

        return model;
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
