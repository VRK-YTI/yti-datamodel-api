package fi.vm.yti.datamodel.api.v2.service;

import java.io.File;
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
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;


public class JSONValidationService {
	public static ValidationRecord validateJSON(InputStream metaSchemaFile, byte[] validationSchemaFile) throws Exception, IOException {
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		ObjectMapper mapper = new ObjectMapper();

		JsonNode validationSchemaNode = mapper.readTree(validationSchemaFile);
		JsonNode metaSchemaNode = mapper.readTree(metaSchemaFile);
		
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

}
