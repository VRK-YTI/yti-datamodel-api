package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;

class JSONSchemaTest {

    @Test
    void testJSONSchemaGeneration() throws Exception {
        var model = MapperTestUtils.getModelFromFile("/json-schema-data.ttl");
        var writer = new StringWriter();
        JSONSchemaBuilder.export(writer, model, "en");

        var expected = MapperTestUtils.getJsonString("/json-schema-expected.json");

        JSONAssert.assertEquals(expected, writer.toString(), JSONCompareMode.LENIENT);
    }

    @Test
    void testJSONSchemaValidationSuccess() throws ValidationException {
        var model = MapperTestUtils.getModelFromFile("/json-schema-data.ttl");
        var writer = new StringWriter();
        JSONSchemaBuilder.export(writer, model, "en");

        var schema = SchemaLoader.load(new JSONObject(writer.toString()));
        var jsonToValidate = new JSONObject("""
{
  "class-1": {
    "property-1": "Test",
    "property-2": [1,2,3,4],
    "property-3": [{}]
  }
}
""");

        schema.validate(jsonToValidate);
    }

    @Test
    void testJSONSchemaValidationFailure() throws ValidationException {
        var model = MapperTestUtils.getModelFromFile("/json-schema-data.ttl");
        var writer = new StringWriter();
        JSONSchemaBuilder.export(writer, model, "en");

        var schema = SchemaLoader.load(new JSONObject(writer.toString()));
        var jsonToValidate = new JSONObject("""
{
  "class-1": {
    "property-1": [42],
    "property-2": "invalid-number",
    "property-3": {}
  }
}
""");
        try {
            schema.validate(jsonToValidate);
            throw new RuntimeException("Validation should have failed");
        } catch (ValidationException e) {
            // expected
        }
    }
}
