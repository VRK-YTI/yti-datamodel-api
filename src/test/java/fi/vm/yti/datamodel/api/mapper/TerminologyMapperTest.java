package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import fi.vm.yti.datamodel.api.v2.mapper.TerminologyMapper;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TerminologyMapperTest {

    @Test
    void mapToJenaModel() {
        String graph = "http://uri.suomi.fi/terminology/test";
        var dto = new TerminologyNodeDTO();
        var properties = new TerminologyNodeDTO.TerminologyProperties();
        var label = new TerminologyNodeDTO.LocalizedValue();
        label.setValue("Test");
        label.setLang("en");
        properties.setPrefLabel(List.of(label));

        dto.setUri(graph);
        dto.setProperties(properties);

        var model = TerminologyMapper.mapToJenaModel(graph, dto);
        var resource = model.getResource(graph);

        assertEquals(graph, resource.getURI());
        assertEquals("Test@en", resource.getProperty(RDFS.label).getObject().toString());
    }

    @Test
    void mapTerminologyDTO() {
        var model = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/terminology.ttl");
        assertNotNull(stream);

        RDFDataMgr.read(model, stream, Lang.TURTLE);

        var terminologyDTO = TerminologyMapper.mapToTerminologyDTO(
                "http://uri.suomi.fi/terminology/test/terminological-vocabulary-0", model);

        assertEquals(Map.of("fi", "Testisanasto", "en", "Test terminology"), terminologyDTO.getLabel());
    }
}
