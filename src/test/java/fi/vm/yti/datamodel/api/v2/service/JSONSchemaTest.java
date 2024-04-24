package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.OpenSearchUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.stream.Collectors;

public class JSONSchemaTest {

    @Test
    void testJSONSchemaGeneration() throws Exception {
        var model = MapperTestUtils.getModelFromFile("/json-schema-data.ttl");
        var writer = new StringWriter();
        JSONSchemaBuilder.export(writer, model, "en");

        var expected = MapperTestUtils.getJsonString("/json-schema-expected.json");

        System.out.println(writer);

        JSONAssert.assertEquals(expected, writer.toString(), JSONCompareMode.LENIENT);
    }
}
