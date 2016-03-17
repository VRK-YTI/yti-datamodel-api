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
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

/**
 *
 * @author malonen
 */
public class ContextWriter {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ContextWriter.class.getName());
 
    private static final Map<String, Boolean> config;
    private static final JsonWriterFactory factory;
    static {
        config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        factory = Json.createWriterFactory(config);
    }
    
    private static String createDefaultContext(JsonObjectBuilder context) {
        
        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        //context.add("type","@type");
        //context.add("id","@id");
        contextBuilder.add("@context", context.build());
        
        return jsonObjectToPrettyString(contextBuilder.build());
}
    
    public static String newClassContext(String classID) { 
    
        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = 
                "SELECT ?resource ?resourceName ?datatype "
                + "WHERE { "
                + "{GRAPH ?resourceID { "
                + "?resourceID a ?type . "
                + "BIND(?resourceID as ?resource)"
                + "BIND(afn:localname(?resourceID) as ?resourceName)"
                + "OPTIONAL { ?resourceID a owl:DatatypeProperty . ?resourceID rdfs:range ?datatype . }"
                + "}} UNION{ "
                + "GRAPH ?resourceID {"
                + "?resourceID sh:property ?property . "
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "?property sh:predicate ?predicate . "
                + "BIND(?predicate as ?resource)"
                + "BIND(afn:localname(?predicate) as ?resourceName)"
                + "}}"
                + "}";

        pss.setIri("resourceID", classID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String predicateID = soln.getResource("resource").toString();
            String predicateName = soln.getLiteral("resourceName").toString();
            
            JsonObjectBuilder predicate = Json.createObjectBuilder();
            predicate.add("@id",predicateID);
            
            if(soln.contains("datatype")) {
                predicate.add("@type",soln.getResource("datatype").toString());
            } else {
               /* FIXME: Too bold? */
               predicate.add("@type","@id"); 
            }
                        
            context.add(predicateName,predicate.build());
        }
        
        return createDefaultContext(context);
        
    }
    
    private static String jsonObjectToPrettyString(JsonObject object) {
        
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = factory.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
        
    }
    
    
    public static String newModelContext(String modelID) { 
    
        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = 
                "SELECT ?resource ?resourceName ?datatype "
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "BIND(afn:localname(?resource) as ?resourceName)"
                + "}"
                + "OPTIONAL {"
                + "GRAPH ?class {"
                + "?class sh:property ?property . "
                + "?property sh:predicate ?resource . "
                + "?property sh:datatype ?datatype . "
                + "}"
                + "}"
                + "}";

        pss.setIri("model", modelID);
        pss.setIri("modelPartGraph",modelID+"#HasPartGraph");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        
        logger.info(pss.toString());
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String predicateID = soln.getResource("resource").toString();
            String predicateName = soln.getLiteral("resourceName").toString();
            
            JsonObjectBuilder predicate = Json.createObjectBuilder();
            predicate.add("@id",predicateID);
            
            if(soln.contains("datatype")) {
                predicate.add("@type",soln.getResource("datatype").toString());
            } else {
               /* FIXME: Too bold? */
               predicate.add("@type","@id"); 
            }
                        
            context.add(predicateName,predicate.build());
        }
        
        return createDefaultContext(context);
    }
    
    
}
