package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.CodeListDTO;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class CodeListMapper {

    private CodeListMapper(){
        //static class
    }

    public static Model mapToJenaModel(String graph, CodeListDTO codelistDTO){
        var model = ModelFactory.createDefaultModel();
        var resource = model.createResource(graph)
                .addProperty(RDF.type, SuomiMeta.CodeScheme);
        codelistDTO.getPrefLabel().forEach((lang, val) -> resource.addProperty(RDFS.label, model.createLiteral(val, lang)));
        resource.addProperty(SuomiMeta.publicationStatus, codelistDTO.getStatus().name());
        return model;
    }

    public static CodeListDTO mapToCodeListDTO(String graph, Model model) {
        var dto = new CodeListDTO();
        var resource = model.getResource(graph);
        var label = MapperUtils.localizedPropertyToMap(resource, RDFS.label);
        dto.setPrefLabel(label);
        dto.setId(graph);
        dto.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        return dto;
    }
}
