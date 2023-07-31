package fi.vm.yti.datamodel.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.v2.dto.MSCR;

/* WHAT HAS TO BE DONE, QUESTIONS, PROBLEMS BY 22.06.2023
 * QUESTIONS
 * 1. Do we process arrays of arrays / nested arrays? If so, probably another else if needed. YES. double check that error
 * 2. Can object's properties be arrays? Currently the URI seems to be wrong at some point, which is similar to object_type: see p.3 or PROBLEMS
 * 3. Should datatypeProperties as eg boolean_type from test_json_trimmed have maxCount?
 * 
 * 
 * PROBLEMS
 * 1. Can't figure out how to assign default maxCount = 1 to object properties only
 * 2. Nested array gave an error - need to check
 * 3. There are two nodes with object type, one ending in #root/object_type, the other in just #object_type. What is wrong
 * 3. ANSWERED: they are nodeShape and propertyShape. propertyShape describes nodeShape and depends on it, so it contains reference
 * ... to nodeShape
 * 
 * TO DOs
 * 1. allOf, anyOf properties. format property - for that one a separate ticket is planned
 */

@Service
public class SchemaService {

	private final Map<String, Resource> XSDTypesMap = Map.ofEntries(Map.entry("string", XSD.xstring),
			Map.entry("number", XSD.xfloat), Map.entry("integer", XSD.integer), Map.entry("boolean", XSD.xboolean),
			Map.entry("null", MSCR.NULL));

	private final Map<String, String> JSONSchemaToSHACLMap = Map.ofEntries(
			Map.entry("description", SHACL.description.getURI()), Map.entry("default", SHACL.defaultValue.getURI()),
			Map.entry("title", SHACL.name.getURI()), Map.entry("additionalProperties", SHACL.closed.getURI()),
			Map.entry("enum", SHACL.in.getURI()),
//			Map.entry("format", SHACL.format.getURI()), -- no SHACL type - look into it
			Map.entry("maximum", SHACL.maxInclusive.getURI()), Map.entry("minimum", SHACL.minInclusive.getURI()),
			Map.entry("exclusiveMaximum", SHACL.maxExclusive.getURI()),
			Map.entry("exclusiveMinimum", SHACL.minExclusive.getURI()), Map.entry("maxItems", SHACL.maxCount.getURI()),
			Map.entry("minItems", SHACL.minCount.getURI()), Map.entry("maxLength", SHACL.maxLength.getURI()),
			Map.entry("minLength", SHACL.minLength.getURI()), Map.entry("not", SHACL.not.getURI()),
			Map.entry("pattern", SHACL.pattern.getURI()) 

	);

	private final Set<String> JSONSchemaNumericalProperties = Set.of("maximum", "minimum", "exclusiveMaximum",
			"exclusiveMinimum", "maxItems", "minItems");
	
	private final Set<String> JSONSchemaBooleanProperties = Set.of("additionalProperties");

	private void checkAndAddPropertyFeature(JsonNode node, Model model, Resource propertyResource) {
		for (var key : JSONSchemaToSHACLMap.keySet()) {
			JsonNode propertyNode = node.findValue(key);
			// the second condition ensures that an object's children properties are not added to that object
			if (propertyNode != null & node.has(key)) {
				if (JSONSchemaNumericalProperties.contains(key)) {
					propertyResource.addProperty(model.getProperty(JSONSchemaToSHACLMap.get(key)),
							model.createTypedLiteral(propertyNode.numberValue()));
				} else if (JSONSchemaBooleanProperties.contains(key)) {
					propertyResource.addProperty(model.getProperty(JSONSchemaToSHACLMap.get(key)),
							model.createTypedLiteral(propertyNode.asBoolean()));
				} 
//				else if(key == "null") {
//					propertyResource.addProperty(model.getProperty(JSONSchemaToSHACLMap.get(key)),
//							model.createTypedLiteral(propertyNode()));
//				}
				else if(key == "enum")
					if (!propertyNode.isEmpty()) {
						RDFNode[] elements = new RDFNode[propertyNode.size()];
						Bag bag = model.createBag();
						
						for(int i = 0; i < propertyNode.size(); i++){
							if (node.get("type").asText() == "boolean")
								elements[i] = model.createTypedLiteral(propertyNode.get(i).asBoolean()); 
							else if (node.get("type").asText() == "integer")
								elements[i] = model.createTypedLiteral(propertyNode.get(i).numberValue());
							else 
								elements[i] = model.createLiteral(propertyNode.get(i).asText());
							bag.add(elements[i]);
						}	
						
						propertyResource.addProperty(model.getProperty(SHACL.in.getURI()),
							bag);
			} 
				else {
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
		propertyResource.addProperty(RDF.type, model.getResource(SHACL.PropertyShape.getURI()));
		propertyResource.addProperty(DCTerms.type, OWL.DatatypeProperty);

		propertyResource.addProperty(model.getProperty(SHACL.datatype.getURI()), XSDTypesMap.get(type));

		propertyResource.addProperty(model.getProperty(SHACL.path.getURI()), propID);

		checkAndAddPropertyFeature(node, model, propertyResource);

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
		Resource propertyResource = addDatatypeProperty(propID + "/" + entry.getKey(), entry.getValue(), model,
				schemaPID, entry.getValue().get("type").asText());
		nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyResource);
		if (!isArrayItem) {
			handleRequiredProperty(entry.getValue(), model, propertyResource, isRequired);
		} 
	}
	
	private String capitaliseNodeIdentifier(String propID) {
		List<String> propIDSplit = new LinkedList<String>(Arrays.asList(propID.split("/")));
		String partToCapitalise = propIDSplit.get(propIDSplit.size() - 1);
		return ((String.join("/", propIDSplit)) + "/" + partToCapitalise.substring(0, 1).toUpperCase() + partToCapitalise.substring(1));
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
	private void handleObject(String propID, JsonNode node, String schemaPID, Model model,
			ArrayList<String> requiredProperties) {

		String propIDCapitalised = capitaliseNodeIdentifier(propID);
		String nameProperty = propID.substring(propID.lastIndexOf("/") + 1);
		Resource nodeShapeResource = model.createResource(schemaPID + "#" + propIDCapitalised);
		
		nodeShapeResource.addProperty(RDF.type, model.getResource(SHACL.NodeShape.getURI()));
		
		
		nodeShapeResource.addProperty(MSCR.localName, nameProperty);
		nodeShapeResource.addProperty(model.getProperty(SHACL.name.getURI()), nameProperty);
		if (node.has("description"))
			nodeShapeResource.addProperty(DCTerms.description, node.get("description").asText());
		if (node == null || node.get("properties") == null) {
			return;
		}
		
		if (node.has("additionalProperties")) {
			nodeShapeResource.addProperty(SH.closed, model.createTypedLiteral(!node.get("additionalProperties").asBoolean()));
		}
		/*
		 * Iterate over properties If a property is an array or object – add and
		 * recursively iterate over them If a property is a datatype / literal – it's
		 * just added.
		 */

		Iterator<Entry<String, JsonNode>> propertiesIterator = node.get("properties").fields();
		while (propertiesIterator.hasNext()) {
			Entry<String, JsonNode> entry = propertiesIterator.next();
			
			if (entry.getKey().startsWith("_") || entry.getKey().startsWith("$"))
				continue;

			if (entry.getValue().get("type") == null) {
				throw new RuntimeException("One of the entries is missing 'type' property");
			}

			if (entry.getValue().get("type").asText().equals("object")) {
				ArrayList<String> updatedRequiredProperties = new ArrayList<>(requiredProperties);

				if (entry.getValue().get("required") != null) {
					entry.getValue().get("required").elements()
							.forEachRemaining(el -> updatedRequiredProperties.add(el.asText()));
				}

				Resource propertyShape = addObjectProperty(propIDCapitalised + "/" + entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propIDCapitalised + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				
				handleObject(propIDCapitalised + "/" + entry.getKey(), entry.getValue(), schemaPID, model,
						updatedRequiredProperties);

			} else if (entry.getValue().get("type").asText().equals("array")
					&& entry.getValue().get("items").has("type")
					&& entry.getValue().get("items").get("type").asText().equals("object")) {
				Resource propertyShape = addObjectProperty(propIDCapitalised + "/" + entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propIDCapitalised + "/" + entry.getKey());
				
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);
				handleObject(propIDCapitalised + "/" + entry.getKey(), entry.getValue().get("items"), schemaPID, model,
						requiredProperties);

			} else if (entry.getValue().get("type").asText().equals("array")) {
				Resource propertyShape = addObjectProperty(propIDCapitalised + "/" + entry.getKey(), entry.getValue(), model, schemaPID,
						schemaPID + "#" + propIDCapitalised + "/" + entry.getKey());
				nodeShapeResource.addProperty(model.getProperty(SHACL.property.getURI()), propertyShape);

				Entry<String, JsonNode> arrayItem = Map.entry(entry.getKey(), entry.getValue().get("items"));

				handleDatatypeProperty(propIDCapitalised, arrayItem, model, schemaPID, nodeShapeResource, false, true);
				
			} else {
				// need to check later if requiredProperties do anything at all in this clause
				boolean isRequired = requiredProperties.contains(entry.getKey()) || (entry.getValue().has("required") && (entry.getValue().get("required").asBoolean() == true));
				handleDatatypeProperty(propIDCapitalised, entry, model, schemaPID, nodeShapeResource, isRequired, false);
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
