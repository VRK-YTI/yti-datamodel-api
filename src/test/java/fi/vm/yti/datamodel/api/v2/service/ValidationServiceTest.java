package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

@ExtendWith(SpringExtension.class)

public class ValidationServiceTest {
	
	@Test
	void testValidationJSON() throws Exception, IOException {
		
		System.out.println("TEST STARTED");
		
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		ObjectMapper mapper = new ObjectMapper();
		// 1. Read schema file from the test resources. Eg using files that are there already
//		String testSchemaPath = "/mscr-datamodel-api/src/test/resources/schema_v4";
		String testSchemaPath = "schema_v4";
//		String metaSchemaPath = "/mscr-datamodel-api/src/test/resources/test_jsonschema_b2share.json";
		String metaSchemaPath = "test_jsonschema_b2share.json";
		System.out.println("STARTING INPUT STREAMS");
		
		InputStream testSchemaInputStream = getClass().getResourceAsStream(testSchemaPath);
//		JsonNode metaSchemaNode = mapper.readTree(new File(metaSchemaPath));
		JsonNode testSchemaNode = mapper.readTree(testSchemaInputStream);
		System.out.println("INPUT STREAM 1 READ");
		
		// 2. Read the metaschema file from the test resources (add it there)
		InputStream metaSchemaInputStream = getClass().getResourceAsStream(metaSchemaPath);
		JsonNode metaSchemaNode = mapper.readTree(metaSchemaInputStream);
		
		// 3. Validate the schema using the test schema, check 
		System.out.println("VALIDATION STARTS");
		JsonSchema schema = schemaFactory.getSchema(metaSchemaNode);
        Set<ValidationMessage> validationResult = schema.validate(testSchemaNode);
        if (validationResult.isEmpty()) {
            System.out.println("no validation errors :-)");
        } else {
            validationResult.forEach(vm -> System.out.println(vm.getMessage()));
        }
        
        System.out.println("TEST ENDED");
	}

}
