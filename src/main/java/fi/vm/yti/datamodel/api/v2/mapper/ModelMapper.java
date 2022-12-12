package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.*;

import java.util.Map;

public class ModelMapper {

    Map<String, String> prefixes = Map.of(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "dcterms", "http://purl.org/dc/terms/",
            "owl", "http://www.w3.org/2002/07/owl#"
    );

    // TODO: fetch these from the database
    Map<String, String> groupMap = Map.of(
              "P11", "v1105"
    );

    public Model mapToJenaModel(DataModelDTO modelDTO) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(prefixes);

        Resource modelResource = model.createResource(modelDTO.getId())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(OWL.versionInfo, modelDTO.getStatus().name());

        modelDTO.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        addLocalizedProperty(modelDTO.getLabel(), modelResource, RDFS.label, model);
        addLocalizedProperty(modelDTO.getDescription(), modelResource, RDFS.comment, model);

        modelDTO.getGroups().forEach(group -> {
            String groupId = groupMap.get(group);
            if (groupId != null) {
                model.createResource("http://urn.fi/URN:NBN:fi:au:ptvl:" + groupId)
                        .addProperty(DCTerms.identifier, group)
                        .addProperty(RDFS.label, model.createLiteral("Group todo label", "fi"));
                modelResource.addProperty(DCTerms.isPartOf, group);
            }
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
