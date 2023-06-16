package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.v2.dto.MSCR;

@Service
public class SchemaService {

	// null -> blank node
	private final Map<String, Resource> XSDTypesMap = Map.ofEntries(Map.entry("string", XSD.xstring),
			Map.entry("number", XSD.xfloat), Map.entry("integer", XSD.integer), Map.entry("boolean", XSD.xboolean),
			Map.entry("null", MSCR.NULL));
	
	private final Map<String, String> JSONSchemaFeaturesToSHACL = Map.ofEntries(
			Map.entry("description", SHACL.description.getURI()),
//			Map.entry("title", SHACL.title??.getURI()), -- ??? how would we call it in shacl?
//			Map.entry("pattern", SHACL.enum??.getURI()), -- does it even belong here? move to objectsHandler? No SHACL type - look into it
//			Map.entry("format", SHACL.format.getURI()), -- no SHACL type - look into it
			Map.entry("maximum", SHACL.maxInclusive.getURI()),
			Map.entry("exclusiveMaximum", SHACL.maxExclusive.getURI()),
			Map.entry("minimum", SHACL.minInclusive.getURI()),
			Map.entry("inclusiveMaximum", SHACL.minExclusive.getURI()),
			Map.entry("maxLength", SHACL.maxLength.getURI()),
			Map.entry("minLength", SHACL.minLength.getURI()),
			Map.entry("pattern", SHACL.pattern.getURI()) // Does it belong here? --> should this map be shared between DataTypeProperty and ..Object, or each have their own?
														 // eg for iteration over 
			
			);
	
	
	private void checkAndAddPropertyFeature(JsonNode node, Model model, Resource propertyResource, String property) {
		JsonNode propertyFeature = node.findValue(property);
		System.out.println("PROPERTY FEATURE " + propertyFeature);
		if (propertyFeature != null) {
			propertyResource.addProperty(model.getProperty(SHACL.description.getURI()), propertyFeature.asText());
		}
	}
	
	/**
	 * Adds a datatype property to the RDF model.
	 * 
	 * @param propID    The property ID.
	 * @param node      The JSON node containing the property details.
	 * @param model     The RDF model.
	 * @param schemaPID The schema PID.
	 * @return The created resource representing the datatype property.
	 */
	private Resource addDatatypeProperty(String propID, JsonNode node, Model model, String schemaPID, String type) {
		Resource propertyResource = model.createResource(schemaPID + "#" + propID);
		System.out.println("propdID: " + propID);
		System.out.println("PROPERTY RESOURCE: " + propertyResource);
		System.out.println("DATATYPE_P NODE: " + node);
//		System.out.println(node.get("description"));
		System.out.println(node.findValue("description"));
		
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.DatatypeProperty);

		propertyResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);

		propertyResource.addProperty(model.getProperty(SHACL.datatype.getURI()), XSDTypesMap.get(type));

//		propertyResource.addProperty(model.getProperty(SHACL.datatype.getURI()), XSD.decimal);
		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);
		
//		JsonNode propertyDescription = node.findValue("description");
//		if (propertyDescription != null) {
//			propertyResource.addProperty(model.getProperty(SHACL.description.getURI()), propertyDescription.asText());
//		}
		checkAndAddPropertyFeature(node, model, propertyResource, "description");
		
	
		
		
		
		return propertyResource;

	}

	/**
	 * 
	 * Adds an object property to the RDF model.
	 * 
	 * @param propID      The property ID.
	 * @param node        The JSON node containing the property details.
	 * @param model       The RDF model.
	 * @param schemaPID   The schema PID.
	 * @param targetShape The target shape for the object property.
	 * @return The created resource representing the object property.
	 */
	private Resource addObjectProperty(String propID, JsonNode node, Model model, String schemaPID,
			String targetShape) {
		Resource propertyResource = model.createResource(schemaPID + "#" + propID);
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.ObjectProperty);
		if (node.get("type").asText().equals("array")) {
			if (node.has("maxItems"))
				propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()),
						model.createTypedLiteral(node.get("maxItems").asInt()));
			if (node.has("minItems"))
				propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()),
						model.createTypedLiteral(node.get("minItems").asInt()));

		} else {
			// ?? Do not remove - default values for objects and arrays??
			propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(1));
			propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()), model.createTypedLiteral(1));

		}
		propertyResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.node.getURI()), model.createResource(targetShape));

		return propertyResource;

	}

	// ONLY HANDLES TYPE !!! ---> NO! even though type is explicitly requested here (because it will be required/enforced), but other properties are handled within the method
	private void handleDatatypeProperty(String propID, Entry<String, JsonNode> entry, Model model, String schemaPID,
			Resource nodeShapeResource) {
		Resource propertyShape = addDatatypeProperty(propID + "/" + entry.getKey(), entry.getValue(), model, schemaPID,
				entry.getValue().get("type").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
	}

	/**
	 * 
	 * Handles an object property and creates the corresponding SHACL (Node)Shape.
	 * 
	 * @param propID    The property ID.
	 * @param node      The JSON node containing the property details.
	 * @param schemaPID The schema PID.
	 * @param model     The RDF model.
	 */
	private void handleObject(String propID, JsonNode node, String schemaPID, Model model) {
		
		System.out.println("\n propdID: " + propID);
//		System.out.println("\n node: " + node);
		System.out.println("\n schemaPID: " + schemaPID);
		System.out.println("\n model: " + model);

		Resource nodeShapeResource = model.createResource(schemaPID + "#" + propID);
		nodeShapeResource.addProperty(RDF.type, model.getResource(SHACL.NodeShape.getURI()));
		nodeShapeResource.addProperty(MSCR.localName, propID);
		if (node.has("description"))
			nodeShapeResource.addProperty(DCTerms.description, node.get("description").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		if (node == null || node.get("properties") == null) {
//		if(node == null) {
			System.out.println("is node == null ?? " + (node == null));
			System.out.println("is node.get(\"properties\") == null ?? " + (node.get("properties") == null));
			System.out.println("RETURNING");
			return;
		}

		/*
		 * Iterate over properties If a property is an array or object – add and
		 * recursively iterate over them If a property is a datatype / literal – it's
		 * just added.
		 */

		Iterator<Entry<String, JsonNode>> propertiesIterator = node.get("properties").fields();
//		System.out.println(propertiesIterator.hasNext() + " NODE PROPS " + node.get("properties"));
		while (propertiesIterator.hasNext()) {
//			System.out.println("NODE PROPERTIES" + node.get("properties"));
			Entry<String, JsonNode> entry = propertiesIterator.next();
			if (entry.getKey().startsWith("_") || entry.getKey().startsWith("$"))
				continue;

			System.out.println("\n\nVALUE: " + entry.getValue() + "||" + " KEY: " + entry.getKey());

			// THROW ERR OR STH – ENFORCE NON NULL
			if (entry.getValue().get("type") == null) {
				System.out.println("NULL?????" + entry.getValue());
			}
			if (entry.getValue().get("type") != null && entry.getValue().get("type").asText().equals("object")) {
				// add object property
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				handleObject(propID + "/" + entry.getKey(), entry.getValue(), schemaPID, model);
			}
			// handles array with "object" items. doesn't handle any other, eg arrays with
			// strings etc
			else if (entry.getValue().get("type") != null && entry.getValue().get("type").asText().equals("array")
					&& entry.getValue().get("items").has("type")
					&& entry.getValue().get("items").get("type").asText().equals("object")) {

				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				handleObject(propID + "/" + entry.getKey(), entry.getValue().get("items"), schemaPID, model);

			}
			// DOUBLE CHECK THAT. ROUGHT IMPLEMENTATION
			// array === one resource (parent) connected to multiple resources (array
			// elements) in SHACL???
			else if (entry.getValue().get("type") != null && entry.getValue().get("type").asText().equals("array")) {
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				System.out.println("\nentry.getValue().get(\"items\"):" + (entry.getValue().get("items")));
				System.out.println("\nentry.getKey()" + (entry.getKey()));
				
//				handleObject(propID + "/" + entry.getKey(), entry.getValue().get("items"), schemaPID, model);
				Entry<String, JsonNode> ArrayItem = Map.entry(entry.getKey() + "/items", entry.getValue().get("items"));
				
				handleDatatypeProperty(propID, ArrayItem, model, schemaPID, nodeShapeResource);
				
			} else {
				System.out.println("PRIMITIVE, KEY: " + entry.getKey());
				System.out.println("PRIMITIVE, VALUE: " + entry.getValue());
				handleDatatypeProperty(propID, entry, model, schemaPID, nodeShapeResource);
			}

		}
	}

	/**
	 * Transforms a JSON schema into an internal RDF model.
	 *
	 * @param schemaPID The schema PID.
	 * @param data      The byte array containing the JSON schema data that comes in
	 *                  request
	 * @return The transformed RDF model.
	 * @throws Exception   If an error occurs during the transformation process.
	 * @throws IOException If an I/O error occurs while reading the JSON schema
	 *                     data.
	 */
	public Model transformJSONSchemaToInternal(String schemaPID, byte[] data) throws Exception, IOException {

		Model model = ModelFactory.createDefaultModel();

		// ObjectMapper is required to parse the JSON data
		ObjectMapper mapper = new ObjectMapper();

		JsonNode root = mapper.readTree(data);
		var modelResource = model.createResource(schemaPID);
		modelResource.addProperty(DCTerms.language, "en");

		// Adding the schema to a corresponding internal model
		handleObject("root", root, schemaPID, model);
		return model;

	}
}
