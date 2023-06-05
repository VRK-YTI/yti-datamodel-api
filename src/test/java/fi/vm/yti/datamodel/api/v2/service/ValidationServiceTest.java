package fi.vm.yti.datamodel.api.v2.service;

import java.io.File;
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
		
		
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		ObjectMapper mapper = new ObjectMapper();
		// 1. Read schema and metaschema files from the test resources
		String metaSchemaPath = "schema_v4";
		String testSchemaPath = "test_jsonschema_b2share.json";
		
		InputStream testSchemaInputStream = getClass().getClassLoader().getResourceAsStream(testSchemaPath);
		InputStream metaSchemaInputStream = getClass().getClassLoader().getResourceAsStream(metaSchemaPath);
		
		JsonNode testSchemaNode = mapper.readTree(testSchemaInputStream);
		JsonNode metaSchemaNode = mapper.readTree(metaSchemaInputStream);
		
		
		// 2. Validate the schema using the test schema, check 
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
