package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.CodeListDTO;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeListMapperTest {

    @Test
    void mapTerminologyToJenaModel() {
        String graph = "http://uri.suomi.fi/codelist/test/testlist";
        var dto = new CodeListDTO();
        dto.setId(graph);
        dto.setStatus(Status.DRAFT);
        dto.setPrefLabel(Map.of("en", "Test"));

        var model = CodeListMapper.mapToJenaModel(graph, dto);
        var resource = model.getResource(graph);

        assertEquals(graph, resource.getURI());
        assertEquals("Test@en", resource.getProperty(RDFS.label).getObject().toString());
        assertEquals(Status.DRAFT, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
    }

    @Test
    void mapTerminologyDTO() {
        var model = MapperTestUtils.getModelFromFile("/codelist.ttl");

        var dto = CodeListMapper.mapToCodeListDTO(
                "http://uri.suomi.fi/codelist/test/testcodelist", model);

        assertEquals(Map.of("fi", "testkoodisto", "en", "Test codelist"), dto.getPrefLabel());
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("http://uri.suomi.fi/codelist/test/testcodelist", dto.getId());

    }
}
