/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.sparql.resultset.ResultSetPeekable;
import java.io.StringWriter;
import java.math.BigDecimal;
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
    
    private static String createDefaultSchema(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonArrayBuilder required) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("properties", properties.build());
        schema.add("required",required.build());
        
        return jsonObjectToPrettyString(schema.build());
}
    
    public static String newClassSchema(String classID) { 
    
        JsonArrayBuilder required = Json.createArrayBuilder();
        JsonObjectBuilder schema = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String selectClass = 
                "SELECT (group_concat(concat(?label,'@',lang(?label)); separator=\", \" ) as ?title) (group_concat(concat(?comment,'@',lang(?comment)); separator=\", \" ) as ?description) "
                + "WHERE { "
                + "GRAPH ?resourceID { "
                + "?resourceID rdfs:label ?label . "
                + "OPTIONAL { ?resourceID rdfs:comment ?comment . }"
                + "} "
                + "} ";
        
        pss.setIri("resourceID", classID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setCommandText(selectClass);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String title = soln.getLiteral("title").getString();
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                schema.add("description", description);
            }
            schema.add("id",classID+".jschema");
            schema.add("title", title);
            
            
        }
        
        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonObjectBuilder contextDef = Json.createObjectBuilder();
        
        contextDef.add("type", "string");
        contextDef.add("format","uri");
        contextDef.add("default",classID+".context");
        properties.add("@context", contextDef.build());
        
        
        String selectResources = 
                "SELECT ?predicate ?predicateName ?datatype ?shapeRef ?min ?max (group_concat(concat(?label,'@',lang(?label)); separator=\", \" ) as ?title) (group_concat(concat(?comment,'@',lang(?comment)); separator=\", \" ) as ?description)"
                + "WHERE { "
                + "GRAPH ?resourceID {"
                + "?resourceID sh:property ?property . "
                + "?property sh:predicate ?predicate . "
                + "?property rdfs:label ?label . "
                + "OPTIONAL { ?property rdfs:comment ?comment . }"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "} GROUP BY ?predicate ?predicateName ?datatype ?shapeRef ?min ?max";
        
        
        pss.setCommandText(selectResources);

        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();

        if(!results.hasNext()) return null;
        

        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            //String predicateID = soln.getResource("predicate").toString();
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
                predicate.add("type",DATATYPE_MAP.get(datatype));
                if(FORMAT_MAP.containsKey(datatype)) {
                    predicate.add("format",FORMAT_MAP.get(datatype));
                }
            } else {
               String shapeRef = soln.getResource("shapeRef").toString();
               predicate.add("type","object");
               predicate.add("$ref",shapeRef+".jschema");
            }
                        
            properties.add(predicateName,predicate.build());
        }
        
        return createDefaultSchema(schema, properties, required);
        
    }
    
    private static String jsonObjectToPrettyString(JsonObject object) {
        
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = factory.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
        
    }
    
    
    public static String newModelSchema(String modelID) { 
    
        JsonObjectBuilder schema = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String selectClass = 
                "SELECT (group_concat(concat(?label,'@',lang(?label)); separator=\", \" ) as ?title) (group_concat(concat(?comment,'@',lang(?comment)); separator=\", \" ) as ?description) "
                + "WHERE { "
                + "GRAPH ?modelID { "
                + "?modelID rdfs:label ?label . "
                + "OPTIONAL { ?modelID rdfs:comment ?comment . }"
                + "} "
                + "} ";
        
        pss.setIri("modelID", modelID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setCommandText(selectClass);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String title = soln.getLiteral("title").getString();
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                schema.add("description", description);
            }
            schema.add("id",modelID+".jschema");
            schema.add("title", title);
            
            
        }
        
        /*JsonObjectBuilder contextDef = Json.createObjectBuilder();
        
        contextDef.add("type", "string");
        contextDef.add("format","uri");
        contextDef.add("default",modelID+".context");
        properties.add("@context", contextDef.build());
        */
        
        String selectResources = 
                "SELECT ?resource ?className (group_concat(concat(?classLabel,'@',lang(?classLabel)); separator=\", \" ) as ?classTitle) (group_concat(concat(?classComment,'@',lang(?classComment)); separator=\", \" ) as ?classDescription) ?predicate ?predicateName ?datatype ?shapeRef ?min ?max (group_concat(concat(?label,'@',lang(?label)); separator=\", \" ) as ?title) (group_concat(concat(?comment,'@',lang(?comment)); separator=\", \" ) as ?description)"
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource rdfs:label ?classLabel . "
                + "OPTIONAL { ?resource rdfs:comment ?classComment . }"
                + "?resourceID sh:property ?property . "
                + "?property sh:predicate ?predicate . "
                + "?property rdfs:label ?label . "
                + "BIND(afn:localname(?resourceID) as ?className)"
                + "OPTIONAL { ?property rdfs:comment ?comment . }"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "} GROUP BY ?resource ?className ?classLabel ?classComment ?predicate ?predicateName ?datatype ?shapeRef ?min ?max";
        
        
        pss.setIri("modelPartGraph", modelID+"#HasPartGraph");
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
            //String predicateID = soln.getResource("predicate").toString();
            
            String className = soln.getLiteral("className").toString();
            String predicateName = soln.getLiteral("predicateName").toString();
            String title = soln.getLiteral("title").toString();
            
            logger.info(className);
            
            
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
                predicate.add("type",DATATYPE_MAP.get(datatype));
                if(FORMAT_MAP.containsKey(datatype)) {
                    predicate.add("format",FORMAT_MAP.get(datatype));
                }
            } else {
               String shapeRef = soln.getResource("shapeRef").toString();
               predicate.add("type","object");
               predicate.add("$ref",shapeRef+".jschema");
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
