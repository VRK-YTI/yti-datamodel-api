package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.TerminologyDTO;
import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

public class TerminologyMapper {

    private TerminologyMapper() {
    }

    public static Model mapToJenaModel(String graph, TerminologyNodeDTO terminologyDTO) {
        Model model = ModelFactory.createDefaultModel();
        var resource = model.createResource(graph);

        resource.addProperty(RDF.type, SKOS.ConceptScheme);
        terminologyDTO.getProperties().getPrefLabel().forEach(label ->
                resource.addProperty(RDFS.label, model.createLiteral(label.getValue(), label.getLang())));

        return model;
    }

    public static TerminologyDTO mapToTerminologyDTO(String graph, Model model) {
        var dto = new TerminologyDTO(graph);
        var label = MapperUtils.localizedPropertyToMap(model.getResource(graph), RDFS.label);
        dto.setLabel(label);
        return dto;
    }
}
