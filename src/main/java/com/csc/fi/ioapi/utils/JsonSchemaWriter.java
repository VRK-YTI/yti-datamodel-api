/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

/**
 *
 * @author malonen
 */
public class JsonSchemaWriter {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(JsonSchemaWriter.class.getName());
    
    public static final Map<String, String> DATATYPE_MAP = 
    Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("http://www.w3.org/2001/XMLSchema#int", "integer");
        put("http://www.w3.org/2001/XMLSchema#integer", "integer");
        put("http://www.w3.org/2001/XMLSchema#long", "integer");
        put("http://www.w3.org/2001/XMLSchema#float", "number");
        put("http://www.w3.org/2001/XMLSchema#double", "number");
        put("http://www.w3.org/2001/XMLSchema#decimal", "number");
        put("http://www.w3.org/2001/XMLSchema#boolean", "boolean");
        put("http://www.w3.org/2001/XMLSchema#date", "string");
        put("http://www.w3.org/2001/XMLSchema#dateTime", "string");
        put("http://www.w3.org/2001/XMLSchema#time", "string");
        put("http://www.w3.org/2001/XMLSchema#gYear", "string");
        put("http://www.w3.org/2001/XMLSchema#gMonth", "string");
        put("http://www.w3.org/2001/XMLSchema#gDay", "string");
        put("http://www.w3.org/2001/XMLSchema#string", "string");
        put("http://www.w3.org/2001/XMLSchema#anyUri", "string");
        put("http://www.w3.org/2001/XMLSchema#langString", "string");
        put("http://www.w3.org/2001/XMLSchema#anyUri", "string");
    }});
    
    public static final Map<String, String> FORMAT_MAP = 
    Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("http://www.w3.org/2001/XMLSchema#dateTime", "date-time");
        put("http://www.w3.org/2001/XMLSchema#anyUri", "uri");
    }});
 
    private static final Map<String, Boolean> config;
    private static final JsonWriterFactory factory;
    static {
        config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        factory = Json.createWriterFactory(config);
    }
    
    
    private static String createDummySchema(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonArrayBuilder required) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("properties", properties.build());
        
        return jsonObjectToPrettyString(schema.build());
  }
    
    
    private static String createDefaultSchema(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonArrayBuilder required) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("properties", properties.build());
        schema.add("required",required.build());
        
        return jsonObjectToPrettyString(schema.build());
}
    
    public static String newClassSchema(String classID, String lang) { 
    
        JsonArrayBuilder required = Json.createArrayBuilder();
        JsonObjectBuilder schema = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String selectClass = 
                "SELECT ?type ?label ?description "
                + "WHERE { "
                + "GRAPH ?resourceID { "
                + "?resourceID a ?type . "
                + "?resourceID rdfs:label ?label . "
                + "FILTER (langMatches(lang(?label),?lang))"
                + "OPTIONAL { ?resourceID rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "} "
                + "} ";
        
        pss.setIri("resourceID", classID);
        pss.setLiteral("lang",lang);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setCommandText(selectClass);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        boolean classMetadata = false;
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String title = soln.getLiteral("label").getString();
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                schema.add("description", description);
            }
            
            schema.add("id",classID+".jschema");
            schema.add("title", title);
            
            logger.info(soln.getResource("type").getLocalName());
            String sType = soln.getResource("type").getLocalName();
            
            if(sType.equals("Class") || sType.equals("Shape")) {
                classMetadata = true;
            }
            
            
        }

        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonObjectBuilder contextDef = Json.createObjectBuilder();
        
        contextDef.add("type", "string");
        contextDef.add("format","uri");
        contextDef.add("default",classID+".context");
        properties.add("@context", contextDef.build());
        
         if(classMetadata) {
        
        String selectResources = 
                "SELECT ?predicate ?predicateName ?label ?datatype ?shapeRef ?min ?max"
                + "WHERE { "
                + "GRAPH ?resourceID {"
                + "?resourceID sh:property ?property . "
                + "?property sh:predicate ?predicate . "
                + "?property rdfs:label ?label . "
                + "FILTER (langMatches(lang(?label),?lang))"
                + "OPTIONAL { ?property rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}";
        
        
        pss.setCommandText(selectResources);
        pss.setLiteral("lang",lang);

        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();
        
        
        if(!results.hasNext()) return null;
        

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            //String predicateID = soln.getResource("predicate").toString();
            String predicateName = soln.getLiteral("predicateName").toString();
            String title = soln.getLiteral("label").toString();
            
            JsonObjectBuilder predicate = Json.createObjectBuilder();
            
            predicate.add("title", title);
            
            if(soln.contains("min")) {
                int min = soln.getLiteral("min").getInt();
                if(min>0) {
                    required.add(predicateName);
                }
            } 
            
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                predicate.add("description", description);
            }
            
            if(soln.contains("datatype")) {
                String datatype = soln.getResource("datatype").toString();
                String jsonDatatype = DATATYPE_MAP.get(datatype);
                 
                 if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                        if(soln.contains("min")) predicate.add("minItems",soln.getLiteral("min").getInt());
                        if(soln.contains("max")) predicate.add("maxItems",soln.getLiteral("max").getInt());
                        predicate.add("type", "array");
                        predicate.add("items", Json.createObjectBuilder().add("type", jsonDatatype).build());                    
                } else {
                    predicate.add("type", jsonDatatype);
                }
                
                if(FORMAT_MAP.containsKey(datatype)) {
                    predicate.add("format",FORMAT_MAP.get(datatype));
                }
            } else {
                String shapeRef = soln.getResource("shapeRef").toString();
               
                if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                        if(soln.contains("min")) predicate.add("minItems",soln.getLiteral("min").getInt());
                        if(soln.contains("max")) predicate.add("maxItems",soln.getLiteral("max").getInt());
                        predicate.add("type", "array");
                        predicate.add("items", Json.createObjectBuilder().add("type","object").add("$ref",shapeRef+".jschema").build());                    
                } else {
                    predicate.add("type","object");
                    predicate.add("$ref",shapeRef+".jschema");
                }
            }
                        
            properties.add(predicateName,predicate.build());
        }
        
            return createDefaultSchema(schema, properties, required);
        
        } 
            else 
         {
             /* Return dummy schema if resource is not a class */
            return createDummySchema(schema, properties, required);
         }
        
    }
    
    private static String jsonObjectToPrettyString(JsonObject object) {
        
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = factory.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
        
    }
    
    
    public static String newModelSchema(String modelID,String lang) { 
    
        JsonObjectBuilder schema = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String selectClass = 
                "SELECT ?label ?description "
                + "WHERE { "
                + "GRAPH ?modelID { "
                + "?modelID rdfs:label ?label . "
                + "FILTER (langMatches(lang(?label),?lang))"
                + "OPTIONAL { ?modelID rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "} "
                + "} ";
        
        pss.setIri("modelID", modelID);
        pss.setLiteral("lang",lang);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setCommandText(selectClass);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String title = soln.getLiteral("label").getString();
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                schema.add("description", description);
            }
            schema.add("id",modelID+".jschema");
            schema.add("title", title);
            
            
        }
        
        String selectResources = 
                "SELECT ?resource ?className ?classTitle ?classDescription ?predicate ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max"
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource rdfs:label ?classTitle . "
                + "FILTER (langMatches(lang(?classTitle),?lang))"
                + "OPTIONAL { ?resource rdfs:comment ?classDescription . "
                + "FILTER (langMatches(lang(?classDescription),?lang))"
                + "}"
                + "?resource sh:property ?property . "
                + "?property sh:predicate ?predicate . "
                + "?property rdfs:label ?title . "
                + "FILTER (langMatches(lang(?title),?lang))"
                + "BIND(afn:localname(?resource) as ?className)"
                + "OPTIONAL { ?property rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}"
                + "ORDER BY ?resource";
        
        
        pss.setIri("modelPartGraph", modelID+"#HasPartGraph");
        pss.setLiteral("lang",lang);
        pss.setCommandText(selectResources);
        
        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();
        ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);
        
        if(!pResults.hasNext()) return null;
        
        JsonObjectBuilder definitions = Json.createObjectBuilder();
        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();
        
        while (pResults.hasNext()) {
            QuerySolution soln = pResults.nextSolution();
            
            if(!soln.contains("className")) return null;
            
            String className = soln.getLiteral("className").toString();
            String predicateName = soln.getLiteral("predicateName").toString();
            String title = soln.getLiteral("title").toString();

            JsonObjectBuilder predicate = Json.createObjectBuilder();
            
            predicate.add("title", title);
            
            if(soln.contains("min")) {
                int min = soln.getLiteral("min").getInt();
                if(min>0) {
                    required.add(predicateName);
                }
            } 
            
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                predicate.add("description", description);
            }
            
            if(soln.contains("datatype")) {
                String datatype = soln.getResource("datatype").toString();
                logger.info(datatype);
                String jsonDatatype = DATATYPE_MAP.get(datatype);
                
                
                if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                        if(soln.contains("min")) predicate.add("minItems",soln.getLiteral("min").getInt());
                        if(soln.contains("max")) predicate.add("maxItems",soln.getLiteral("max").getInt());
                        predicate.add("type", "array");
                        if(jsonDatatype!=null) {
                            predicate.add("items", Json.createObjectBuilder().add("type", jsonDatatype).build());
                        }                    
                } else {
                    if(jsonDatatype!=null) {
                        predicate.add("type", jsonDatatype);
                    }
                }
                
                if(FORMAT_MAP.containsKey(datatype)) {
                    predicate.add("format",FORMAT_MAP.get(datatype));
                }
            } else {
                if(soln.contains("shapeRefName")) {
                    String shapeRefName = soln.getLiteral("shapeRefName").toString();

                     if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                             if(soln.contains("min")) predicate.add("minItems",soln.getLiteral("min").getInt());
                             if(soln.contains("max")) predicate.add("maxItems",soln.getLiteral("max").getInt());
                             predicate.add("type", "array");
                             predicate.add("items", Json.createObjectBuilder().add("type","object").add("$ref","#/definitions/"+shapeRefName).build());                    
                     } else {
                         predicate.add("type","object");
                         predicate.add("$ref","#/definitions/"+shapeRefName);
                     }
                }
            }
            
            properties.add(predicateName,predicate.build());
            
            /* Check if next result is about the same class */
           
            
                /* If not build props and requires */
                if(!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").toString())) {
                    JsonObjectBuilder classDefinition = Json.createObjectBuilder();
                    classDefinition.add("title",soln.getLiteral("classTitle").toString());
                    if(soln.contains("classDescription")) {
                        classDefinition.add("description",soln.getLiteral("classDescription").toString());
                    }
                    classDefinition.add("properties", properties.build());
                    classDefinition.add("required", required.build());
                    definitions.add(className, classDefinition.build());
                    properties = Json.createObjectBuilder();
                    required = Json.createArrayBuilder();
                } 
            
            
            
        }
        
        return createDefaultModelSchema(schema, definitions);
        
    }
    
    private static String createDefaultModelSchema(JsonObjectBuilder schema, JsonObjectBuilder definitions) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("definitions", definitions.build());
        
        return jsonObjectToPrettyString(schema.build());
}
    
    
}
