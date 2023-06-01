package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.v2.dto.MSCR;

@Service
public class SchemaService {

	private Resource addDatatypeProperty(String propID, JsonNode node, Model model, String schemaPID) {
		Resource propertyResource = model.createResource(schemaPID + "#" + propID);
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.DatatypeProperty);		
		propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(1));
		propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()), model.createTypedLiteral(1));
		propertyResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.datatype.getURI()), XSD.xstring);
		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);		
		return propertyResource;
		
	}
	
	
	private Resource addObjectProperty(String propID, JsonNode node, Model model, String schemaPID, String targetShape) {
		Resource propertyResource = model.createResource(schemaPID + "#" + propID);
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.ObjectProperty);
		if(node.get("type").asText().equals("array")) {
			if(node.has("maxItems"))
				propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(node.get("maxItems").asInt()));
			if(node.has("minItems"))
				propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()), model.createTypedLiteral(node.get("minItems").asInt()));

		}
		else {
			propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(1));
			propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()), model.createTypedLiteral(1));
			
		}
		propertyResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.node.getURI()), model.createResource(targetShape));
				
		return propertyResource;
		
	}		
	
	
	private void handleObject(String propID, JsonNode node, String schemaPID, Model model) {

		Resource nodeShapeResource = model.createResource(schemaPID + "#" + propID);
		
		nodeShapeResource.addProperty(RDF.type, model.getResource(SHACL.NodeShape.getURI()));
		nodeShapeResource.addProperty(MSCR.localName, propID);
		if(node.has("description"))
			nodeShapeResource.addProperty(DCTerms.description, node.get("description").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		if(node == null || node.get("properties") == null) {
			return;
		}
		Iterator<Entry<String, JsonNode>> propertiesIterator = node.get("properties").fields();
		while(propertiesIterator.hasNext()) {
			Entry<String, JsonNode> entry = propertiesIterator.next();
			if(entry.getKey().startsWith("_") || entry.getKey().startsWith("$"))
				continue;
			//System.out.println(entry.getValue());
			if(entry.getValue().get("type") != null && entry.getValue().get("type").asText().equals("object")) {
				// add object property 
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID, schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				handleObject(propID + "/" + entry.getKey(), entry.getValue(), schemaPID, model);
			}
			else if(entry.getValue().get("type") != null && entry.getValue().get("type").asText().equals("array") && entry.getValue().get("items").has("type") && entry.getValue().get("items").get("type").asText().equals("object")) {
				
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID, schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);	
				handleObject(propID + "/" + entry.getKey(), entry.getValue().get("items"), schemaPID, model);	
				
			}
			else {
				Resource propertyShape = addDatatypeProperty(propID + "/" + entry.getKey(), entry.getValue(), model, schemaPID);
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
			}
			
		}	

	}
	public Model transformJSONSchemaToInternal(String schemaPID, byte[] data) throws Exception, IOException {
		
		// add error handling
		
		
		// add validaiton
		// I have json file content and (meta)schema - schema of json schema.
		// you need to validate against that schema – json-schema draft 4.
		//
		
		
		Model model = ModelFactory.createDefaultModel();
		System.out.println("MODEL: " + model.toString());
		
		ObjectMapper mapper = new ObjectMapper();
		System.out.println("MAPPER: " + mapper);
		
		System.out.println("DATA: " + data);
		
		// The root is a json object containing information that appears to have data about json model\s schema, 
		// and doesn't include data from the uploaded schema file.
		
		// where does it get it from? from json-schema.org reference? what happens with the uploaded data then?
		
		// From method description:
		/* Returns root of the resulting tree (where root can consist
	     * of just a single node if the current event is a
	     * value event, not container).
	     */
		JsonNode root = mapper.readTree(data);
		System.out.println("ROOT: " + root);
		var rootIter = root.fieldNames();
		while (rootIter.hasNext()) {
			var nextThing = rootIter.next();
			System.out.println(nextThing);
		}
//		mapper.
		
		String schemaVersion  =root.get("$schema").asText();
		System.out.println("SCHEMA VERSION: " + schemaVersion);
		
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
		
		String metaSchemaPath = "test_jsonschema_b2share.json";
//		String metaSchemaPath = "/mscr-datamodel-api/src/test/resources/test_jsonschema_b2share.json";
//		String metaSchemaPath = "/mscr-datamodel-api/src/main/resources/schema_v4";
		

		// close stream later?
		InputStream metaSchemaInputStream = getClass().getResourceAsStream(metaSchemaPath);
		
//		JsonNode metaSchemaNode = mapper.readTree(new File(metaSchemaPath));
		JsonNode metaSchemaNode = mapper.readTree(metaSchemaInputStream);
		
        JsonSchema schema = schemaFactory.getSchema(metaSchemaNode);
        Set<ValidationMessage> validationResult = schema.validate(root);
        
        if (validationResult.isEmpty()) {
            System.out.println("no validation errors :-)");
        } else {
            validationResult.forEach(vm -> System.out.println(vm.getMessage()));
        }
		var modelResource = model.createResource(schemaPID);
		System.out.println("Model Resource: " + modelResource);
		modelResource.addProperty(DCTerms.language, "en");
		
		
		handleObject("root", root, schemaPID, model);
			
		
		return model;
		
	}
	
}
