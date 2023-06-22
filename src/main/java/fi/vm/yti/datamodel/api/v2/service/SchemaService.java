package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

	private final Map<String, Resource> XSDTypesMap = Map.ofEntries(Map.entry("string", XSD.xstring),
			Map.entry("number", XSD.xfloat), Map.entry("integer", XSD.integer), Map.entry("boolean", XSD.xboolean),
			Map.entry("null", MSCR.NULL));

	//
	private final Map<String, String> JSONSchemaToSHACLMap = Map.ofEntries(
			Map.entry("description", SHACL.description.getURI()), Map.entry("default", SHACL.defaultValue.getURI()),
			Map.entry("title", SHACL.name.getURI()), Map.entry("additionalProperties", SHACL.closed.getURI()),
			Map.entry("enum", SHACL.in.getURI()),
//			Map.entry("format", SHACL.format.getURI()), -- no SHACL type - look into it
			Map.entry("maximum", SHACL.maxInclusive.getURI()), Map.entry("minimum", SHACL.minInclusive.getURI()),
			Map.entry("exclusiveMaximum", SHACL.maxExclusive.getURI()),
			Map.entry("exclusiveMinimum", SHACL.minExclusive.getURI()), Map.entry("maxItems", SHACL.maxCount.getURI()),
			Map.entry("minItmes", SHACL.minCount.getURI()), Map.entry("maxLength", SHACL.maxLength.getURI()),
			Map.entry("minLength", SHACL.minLength.getURI()), Map.entry("not", SHACL.not.getURI()),
			Map.entry("pattern", SHACL.pattern.getURI()) // Does it belong here? --> should this map be shared
															// between DataTypeProperty and ..Object, or each
															// have their own?
															// eg for iteration over. Yes? As in it doesn't
															// matter, either a node has the property or not

	);

	private final Set<String> JSONSchemaNumericalProperties = Set.of("maximum", "minimum", "exclusiveMaximum",
			"exclusiveMinimum", "maxItems", "minItems");

	private void checkAndAddPropertyFeature(JsonNode node, Model model, Resource propertyResource) {
		for (var key : JSONSchemaToSHACLMap.keySet()) {
			JsonNode propertyNode = node.findValue(key);
			if (propertyNode != null) {
				if (JSONSchemaNumericalProperties.contains(key)) {
					propertyResource.addProperty(model.getProperty(JSONSchemaToSHACLMap.get(key)),
							model.createTypedLiteral(propertyNode.asInt()));
				} else {
					propertyResource.addProperty(model.getProperty(JSONSchemaToSHACLMap.get(key)),
							propertyNode.asText());
				}
			}
		}
	}
	
	private void handleRequiredProperty(JsonNode node, Model model, Resource propertyResource, boolean isRequired) {
		propertyResource.addProperty(model.getProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(1));
		if (isRequired) {
			propertyResource.addProperty(model.getProperty(SHACL.minCount.getURI()), model.createTypedLiteral(1));
		}
	}

//	private boolean hasTypeProperty(JsonNode node) {
//		System.out.println(node.findValue("type"));
//		return (node.findValue("type") != null);
//	};

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
		
		System.out.println("DATATYPE PROP NODE: " + node);

		Resource propertyResource = model.createResource(schemaPID + "#" + propID);
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.DatatypeProperty);

		propertyResource.addProperty(model.getProperty(SHACL.datatype.getURI()), XSDTypesMap.get(type));

		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);

		checkAndAddPropertyFeature(node, model, propertyResource);

		return propertyResource;

	}

	// 1. HANDLE REQUIRED property of OBJECTS

	// 2. Add maxCount = 1. on datatype and object properties. If there's a
	// required, then also min = 1

	//
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
		
		System.out.println("OBJECT PROP NODE: " + node.get("type").asText());
		
//		if (node.get("type").asText().equals("object")) {
//			System.out.println("FOUND OBJECT");
//			handleRequiredProperty(node, model, propertyResource, isRequired);
//		}

		checkAndAddPropertyFeature(node, model, propertyResource);
		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);
		propertyResource.addProperty(model.getProperty(SHACL.node.getURI()), model.createResource(targetShape));

		return propertyResource;

	}

	// ONLY HANDLES TYPE !!! ---> NO! even though type is explicitly requested here
	// (because it will be required/enforced), but other properties are handled
	// within the method
	private void handleDatatypeProperty(String propID, Entry<String, JsonNode> entry, Model model, String schemaPID,
			Resource nodeShapeResource, boolean isRequired, boolean isArrayItem) {
		Resource propertyResource = addDatatypeProperty(propID + "/" + entry.getKey(), entry.getValue(), model, schemaPID,
				entry.getValue().get("type").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyResource);
		if (!isArrayItem) {
			handleRequiredProperty(entry.getValue(), model, propertyResource, isRequired);
		}
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
	private void handleObject(String propID, JsonNode node, String schemaPID, Model model, ArrayList<String> requiredProperties) {

		Resource nodeShapeResource = model.createResource(schemaPID + "#" + propID);
		nodeShapeResource.addProperty(RDF.type, model.getResource(SHACL.NodeShape.getURI()));
		nodeShapeResource.addProperty(MSCR.localName, propID);
		if (node.has("description"))
			nodeShapeResource.addProperty(DCTerms.description, node.get("description").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.name.getURI()), propID);
		if (node == null || node.get("properties") == null) {
			System.out.println("RETURNING");
			return;
		}

		/*
		 * Iterate over properties If a property is an array or object – add and
		 * recursively iterate over them If a property is a datatype / literal – it's
		 * just added.
		 */
		
		// The problem is that if we store names of required properties and later on compare datatype props to them, it can be that
		// object property has the same name as a datatype property

		Iterator<Entry<String, JsonNode>> propertiesIterator = node.get("properties").fields();
		while (propertiesIterator.hasNext()) {
			Entry<String, JsonNode> entry = propertiesIterator.next();
			if (entry.getKey().startsWith("_") || entry.getKey().startsWith("$"))
				continue;

			if (entry.getValue().get("type") == null) {
				System.out.println("NULL?????" + entry.getValue() + "/n" + entry);
				throw new RuntimeException("One of the entries is missing 'type' property");
			}
			
			if (entry.getValue().get("type").asText().equals("object")) {
				System.out.println("HANDLING THE FOLLOWING: " + entry.getKey());
				System.out.println("FINDING VALUES: " + entry.getValue().findValue("required"));
				System.out.println("HANDLING THE FOLLOWING (VALUES): " + entry.getValue().get("properties"));
//				List<String> newRequiredProperties = requiredProperties.isEmpty() ? new ArrayList<String>() :;
				ArrayList<String> updatedRequiredProperties = new ArrayList<>(requiredProperties);
				
				if (entry.getValue().get("required") != null) {
//					System.out.println("REQUIRED: " + entry.getValue().get("required").elements().forEachRemaining(el -> arra.add(el)));
					entry.getValue().get("required").elements().forEachRemaining(el -> updatedRequiredProperties.add(el.asText()));
//					String[] arra = entry.getValue().get("required");
//					entry.getValue().get("required").elements();
					System.out.println("REQUIRED VALUES: " + updatedRequiredProperties.get(0));
				}
				
//				boolean isRequired = updatedRequiredProperties.contains(entry.getKey());
				
				
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				
				handleObject(propID + "/" + entry.getKey(), entry.getValue(), schemaPID, model, updatedRequiredProperties);

			} else if (entry.getValue().get("type").asText().equals("array")
					&& entry.getValue().get("items").has("type")
					&& entry.getValue().get("items").get("type").asText().equals("object")) {

				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				handleObject(propID + "/" + entry.getKey(), entry.getValue().get("items"), schemaPID, model, requiredProperties);

			}
			// DOUBLE CHECK THAT. ROUGHT IMPLEMENTATION
			// array === one resource (parent) connected to multiple resources (array elements) in SHACL???
			// test nested arrays
			else if (entry.getValue().get("type").asText().equals("array")) {
				Resource propertyShape = addObjectProperty(entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propID + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);

				Entry<String, JsonNode> arrayItem = Map.entry(entry.getKey() + "/items", entry.getValue().get("items"));
//				System.out.println("ARRAYITEM KEY: " + arrayItem.getKey() + " ARRAYITEM VALUE: " + arrayItem.getValue());
				
//				boolean isRequired = requiredProperties.contains(propertyShape);

				handleDatatypeProperty(propID, arrayItem, model, schemaPID, nodeShapeResource, false, true);
//				handleObject(arrayItem.getKey(), arrayItem.getValue(),  schemaPID,  model, nodeShapeResource, requiredProperties);

			} else {
				boolean isRequired = requiredProperties.contains(entry.getKey());
//				System.out.println("is required? " + isRequired);
//				System.out.println("HANDLING THE FOLLOWING PRIMITIVE: " + entry.getKey());
//				System.out.println("HANDLING THE FOLLOWING TYPE: " + entry.getValue().get("type"));
				handleDatatypeProperty(propID, entry, model, schemaPID, nodeShapeResource, isRequired, false);
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
		
		ArrayList<String> requiredProperties = new ArrayList<String>();

		// Adding the schema to a corresponding internal model
		handleObject("root", root, schemaPID, model, requiredProperties);
		return model;

	}
}
