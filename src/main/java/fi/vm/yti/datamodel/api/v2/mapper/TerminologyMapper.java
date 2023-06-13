package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ConceptDTO;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.dto.TerminologyDTO;
import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;

import java.util.List;

public class TerminologyMapper {

    private TerminologyMapper() {
    }

    public static Model mapTerminologyToJenaModel(String graph, TerminologyNodeDTO terminologyDTO) {
        var model = ModelFactory.createDefaultModel();
        var resource = model.createResource(graph)
                        .addProperty(RDF.type, SKOS.ConceptScheme);

        var prefLabels = terminologyDTO.getProperties().getPrefLabel();
        prefLabels.forEach(label -> resource.addProperty(RDFS.label, model.createLiteral(label.getValue(), label.getLang())));
        return model;
    }

    public static TerminologyDTO mapToTerminologyDTO(String graph, Model model) {
        var dto = new TerminologyDTO(graph);
        var label = MapperUtils.localizedPropertyToMap(model.getResource(graph), RDFS.label);
        dto.setLabel(label);
        return dto;
    }

    public static void mapConceptToTerminologyModel(Model terminologyModel, String terminologyURI, String conceptURI, TerminologyNodeDTO nodeDTO) {
        var resource = terminologyModel.getResource(conceptURI);
        resource.removeAll(SKOS.definition);
        resource.removeAll(SKOS.prefLabel);
        resource.removeAll(OWL.versionInfo);

        resource.addProperty(RDF.type, SKOS.Concept);
        resource.addProperty(SKOS.inScheme, ResourceFactory.createResource(terminologyURI));
        resource.addProperty(OWL.versionInfo,
                getProperty(nodeDTO.getProperties().getStatus(), Status.DRAFT.name()));

        for (var def : nodeDTO.getProperties().getDefinition()) {
            if (def.getValue() == null || def.getValue().isEmpty()) {
                continue;
            }
            resource.addProperty(SKOS.definition,
                    terminologyModel.createLiteral(def.getValue(), def.getLang()));
        }

        nodeDTO.getReferences().getPrefLabelXl().forEach(label -> {
            var prefLabel = label.getProperties().getPrefLabel();
            var prefLabelValue = getProperty(prefLabel, null);
            if (prefLabelValue != null) {
                var lang = prefLabel.get(0).getLang();
                resource.addProperty(SKOS.prefLabel,
                        terminologyModel.createLiteral(prefLabelValue, lang));
            }
        });
    }

    public static ConceptDTO mapToConceptDTO(Model model, String conceptURI) {
        var resource = model.getResource(conceptURI);

        var dto = new ConceptDTO();
        dto.setConceptURI(conceptURI);

        var status = Status.DRAFT;
        try {
            status = Status.valueOf(MapperUtils.propertyToString(resource, OWL.versionInfo));
        } catch (Exception e) {
            // use default status in case of status is missing or invalid
        }
        dto.setStatus(status);
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
        dto.setDefinition(MapperUtils.localizedPropertyToMap(resource, SKOS.definition));

        var terminology = new TerminologyDTO(MapperUtils.propertyToString(resource, SKOS.inScheme));
        terminology.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setTerminology(terminology);
        return dto;
    }

    private static String getProperty(List<TerminologyNodeDTO.LocalizedValue> value, String defaultValue) {
        if (value != null && !value.isEmpty()) {
            return value.get(0).getValue();
        }
        return defaultValue;
    }
}
