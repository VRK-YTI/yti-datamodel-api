package fi.vm.yti.datamodel.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)

@Import({
    JSONValidationService.class
})

public class JSONValidationServiceTest {
	
	String metaSchemaPath = "schema_v4";
	InputStream metaSchemaInputStream = getClass().getClassLoader().getResourceAsStream(metaSchemaPath);
	
	// have to restart the below one (comment - save - uncomment - save) each time the file is modified
	String secondInvalidInputSchemaPath = "test_jsonschema_invalid_schema.json";
	
	
	private byte[] byteStreamFromPath(String schemaPath) throws Exception, IOException {
		InputStream inputSchemaInputStream = getClass().getClassLoader().getResourceAsStream(schemaPath);
		byte[] inputSchemaInByte = inputSchemaInputStream.readAllBytes();
		inputSchemaInputStream.close();
		
		return inputSchemaInByte;
	}
	
	@Test
	void testValidJSONSchema() throws Exception, IOException {
		String validInputSchemaPath = "test_jsonschema_b2share.json";
		// *** validate format (eg email, date etc) with regex? ***
		
		assertEquals(JSONValidationService.validateJSON(metaSchemaInputStream, byteStreamFromPath(validInputSchemaPath)), new ValidationRecord(true, Arrays.asList()));
		
		
	}
	
	@Test
	void testInvalidJSONSchema() throws Exception, IOException {
		String firstInvalidInputSchemaPath = "test_jsonschema_invalid_types.json";
		assertFalse(JSONValidationService.validateJSON(metaSchemaInputStream, byteStreamFromPath(firstInvalidInputSchemaPath)).isValid());
	}
	
	@Test
	void testInvalidJSONSchemaDetailsType() throws Exception, IOException {
		String firstInvalidInputSchemaPath = "test_jsonschema_invalid_types.json";
		List<String> expectedErrorMessages = Arrays.asList("$.properties.creators.type: does not have a value in the enumeration [array, boolean, integer, null, number, object, string]",
														   "$.properties.creators.type: string found, array expected");
		assertLinesMatch(JSONValidationService.validateJSON(metaSchemaInputStream, byteStreamFromPath(firstInvalidInputSchemaPath)).validationOutput(),
															expectedErrorMessages);
	}
	
	@Test
	void testInvalidJSONSchemaDetailsSchema() throws Exception, IOException {
		String secondInvalidInputSchemaPath = "test_jsonschema_invalid_schema.json";
		List<String> expectedErrorMessages = Arrays.asList("Schema versions don't match!");
		assertLinesMatch(JSONValidationService.validateJSON(metaSchemaInputStream, byteStreamFromPath(secondInvalidInputSchemaPath)).validationOutput(),
															expectedErrorMessages);
	}
	}


