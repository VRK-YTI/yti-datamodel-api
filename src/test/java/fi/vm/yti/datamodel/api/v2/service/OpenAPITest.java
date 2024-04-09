package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;

class OpenAPITest {

    @Test
    void testOpeAPIGeneration() throws Exception {
        var model = MapperTestUtils.getModelFromFile("/open-api-data.ttl");

        var writer = new StringWriter();
        OpenAPIBuilder.export(writer, model, "en");

        var expected = MapperTestUtils.getJsonString("/open-api-expected.json");
        JSONAssert.assertEquals(expected, writer.toString(), JSONCompareMode.LENIENT);
    }
}
