package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;


public class JSONValidationService {
	public static ValidationRecord validateJSON(InputStream metaSchemaFile, byte[] validationSchemaFile) throws Exception, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		JsonNode validationSchemaNode = mapper.readTree(validationSchemaFile);
		JsonNode metaSchemaNode = mapper.readTree(metaSchemaFile);
		
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(metaSchemaNode));
		
		boolean doSchemaVersionsMatch = metaSchemaNode.get("$schema").asText().equals(validationSchemaNode.get("$schema").asText());
		
		JsonSchema schema = schemaFactory.getSchema(metaSchemaNode);
	    Set<ValidationMessage> validationResult = schema.validate(validationSchemaNode);
	    
	    if (validationResult.isEmpty() & doSchemaVersionsMatch) {
	        return new ValidationRecord(true, Arrays.asList());
	    } else {	    	
	    	List<String> errorMessages = new ArrayList<String> ();
	    	
	    	if (!doSchemaVersionsMatch) errorMessages.add("Schema versions don't match!");
	    	
	    	validationResult.forEach(vm -> errorMessages.add(vm.getMessage()));
	    	return new ValidationRecord(false, errorMessages);
	    }
	    
	}
	
	// dictionary URI -> location of a metaschema file
	// in the optimized version - it would be a validator, parsed schema content ...
	// ... for that version so that it can be stored
	public static void SupportedSchemaVersions() {
		return void;
	}

}
