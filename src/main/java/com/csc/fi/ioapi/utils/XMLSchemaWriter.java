/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import static com.csc.fi.ioapi.utils.GraphManager.services;
import com.predic8.schema.Annotation;
import com.predic8.schema.Appinfo;
import com.predic8.schema.Attribute;
import com.predic8.schema.ComplexType;
import com.predic8.schema.Documentation;
import com.predic8.schema.Element;
import com.predic8.schema.Schema;
import com.predic8.schema.Sequence;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.xml.namespace.QName;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.Query;
import org.apache.jena.util.SplitIRI;

/**
 *
 * @author malonen
 */
public class XMLSchemaWriter {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(XMLSchemaWriter.class.getName());
    
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
        
        /* TODO: Create basic dummy schema without properties */
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("properties", properties.build());
        JsonArray reqArray = required.build();
        if(!reqArray.isEmpty()) {
            schema.add("required",reqArray);
        }
        
        return jsonObjectToPrettyString(schema.build());
  }
    
    
    private static String createDefaultSchema(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonArrayBuilder required) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("properties", properties.build());
        JsonArray reqArray = required.build();
        
        if(!reqArray.isEmpty()) {
            schema.add("required",reqArray);
        }
        
        return jsonObjectToPrettyString(schema.build());
}
    

    public static Annotation createAnnotation(String documentation, String lang) {
                Annotation comment = new Annotation();
                Documentation documentationItem = new Documentation();
                documentationItem.setLang(lang);
                //documentation.setProperty("xml:lang", lang);
                documentationItem.setContent(documentation);
                comment.setContents(documentationItem);
        return comment;
    }
   
       
    public static String newClassSchema(String classID, String lang) { 
        logger.info("Creating schema");
        Schema schema = new Schema("urn:uuid:test");
        //   Element newClass = new Element();
        
        String className = SplitIRI.localname(classID);
     
        schema.newElement(className, className+"Type");
        ComplexType newClassType = schema.newComplexType(className+"Type");
        
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
        if(lang!=null) pss.setLiteral("lang",lang);
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
               // seq.newElement("description", new QName("http://www.w3.org/2001/XMLSchema","string"));
                newClassType.setAnnotation(createAnnotation(description,lang));
            }
            
          
         //   logger.info(soln.getResource("type").getLocalName());
            String sType = soln.getResource("type").getLocalName();
            
            if(sType.equals("Class") || sType.equals("Shape")) {
                classMetadata = true;
            }
            
        }

       
        if(classMetadata) {
        
        String selectResources = 
                "SELECT ?predicate ?id ?predicateName ?label ?datatype ?shapeRef ?min ?max ?minLength ?maxLenght ?pattern "
                + "WHERE { "
                + "GRAPH ?resourceID {"
                + "?resourceID sh:property ?property . "
                + "?property sh:predicate ?predicate . "
                + "OPTIONAL { ?property dcterms:identifier ?id . }"
                + "?property rdfs:label ?label . "
                + "FILTER (langMatches(lang(?label),?lang))"
                + "OPTIONAL { ?property rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLenght ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}";
        
        
        pss.setCommandText(selectResources);
        if(lang!=null) pss.setLiteral("lang",lang);

        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();
        
        
        if(!results.hasNext()) return null;
        
        
        Sequence seq = newClassType.newSequence();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            //String predicateID = soln.getResource("predicate").getString();
            String predicateName = soln.getLiteral("predicateName").getString();
            
            logger.info(predicateName);
            
            if(soln.contains("id")) {
                predicateName = soln.getLiteral("id").getString();
            }
            
            
            Element newElement = new Element();
            newElement.setName(predicateName);
            
            String title = soln.getLiteral("label").getString();
            
            JsonObjectBuilder predicate = Json.createObjectBuilder();
            
            predicate.add("title", title);
            
            if(soln.contains("min") ) {
                int min = soln.getLiteral("min").getInt();
                if(min>0) {
                    newElement.setMinOccurs(""+min);
                }
            } 
            
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                //predicate.add("description", description);
                newElement.setAnnotation(createAnnotation(description,lang));
            }
            
            if(soln.contains("datatype")) {
                String datatype = soln.getResource("datatype").toString();
               // String jsonDatatype = DATATYPE_MAP.get(datatype);
            }
            
            if(soln.contains("max") && soln.getLiteral("max").getInt()>0) {
                int max = soln.getLiteral("max").getInt();
                    newElement.setMaxOccurs(""+max);
            }

                    /*
                if(soln.contains("maxLength"))  {
                    predicate.add("maxLength",soln.getLiteral("maxLength").getInt());
                }
                
                if(soln.contains("minLength"))  {
                    predicate.add("minLength",soln.getLiteral("minLength").getInt());
                }
                
                if(soln.contains("pattern"))  {
                    predicate.add("pattern",soln.getLiteral("pattern").getString());
                }*/
                 
                if(soln.contains("shapeRef")) {
                    String shapeRef = soln.getResource("shapeRef").toString();
                    newElement.setRefValue(shapeRef+"Type");
                }
                
            seq.add(newElement);
        }     
        
        }
       
       logger.info(schema.getAsString());
       
        return schema.getAsString();
    }
        
    
    
    private static String jsonObjectToPrettyString(JsonObject object) {
        
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = factory.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
        
    }
    
    public static boolean hasModelRoot(String graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?graph void:rootResource ?root . }}";
        
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(queryString);
        pss.setIri("graph", graphIRI);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    
    public static String getModelRoot(String graph) {
        
         ParameterizedSparqlString pss = new ParameterizedSparqlString();
                String selectResources = 
                "SELECT ?root WHERE {"
                + "GRAPH ?graph { ?graph void:rootResource ?root . }"
                + "}";
        
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        pss.setIri("graph", graph);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());
        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        
        if(!results.hasNext()) return null;
        else {
            QuerySolution soln = results.next();
            if(soln.contains("root")) {
                return soln.getResource("root").toString();
            } else return null;
        }
    }
    
    
    public static String newModelSchema(String modelID, String lang) { 
    
        logger.info("Building JSON Schema from "+modelID);
        
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
        if(lang!=null) pss.setLiteral("lang",lang);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setCommandText(selectClass);
        
        logger.info(""+services.getCoreSparqlAddress());
        logger.info(""+pss);
       
        
        QueryExecution qexec =  QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.toString());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
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
                "SELECT ?resource ?className ?classTitle ?classDescription ?predicate ?id ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
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
                + "OPTIONAL { ?property dcterms:identifier ?id . }"
                + "?property rdfs:label ?title . "
                + "FILTER (langMatches(lang(?title),?lang))"
                + "BIND(afn:localname(?resource) as ?className)"
                + "OPTIONAL { ?property rdfs:comment ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLenght ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}"
                + "ORDER BY ?resource";
        
        
        pss.setIri("modelPartGraph", modelID+"#HasPartGraph");
        if(lang!=null) pss.setLiteral("lang",lang);
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
            
            String className = soln.getLiteral("className").getString();
            
            String predicateName = soln.getLiteral("predicateName").getString();
            
            if(soln.contains("id")) {
                predicateName = soln.getLiteral("id").getString();
            }
            
            String title = soln.getLiteral("title").getString();

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
                

                if(soln.contains("min") && soln.getLiteral("min").getInt()>0) {
                    predicate.add("minItems",soln.getLiteral("min").getInt());
                }
       
                if(soln.contains("maxLength"))  {
                    predicate.add("maxLength",soln.getLiteral("maxLength").getInt());
                }
                
                if(soln.contains("minLength"))  {
                    predicate.add("minLength",soln.getLiteral("minLength").getInt());
                }
                
                if(soln.contains("pattern"))  {
                    predicate.add("pattern",soln.getLiteral("pattern").getString());
                }
                
                if(soln.contains("max") && soln.getLiteral("max").getInt()<=1) {

                    predicate.add("maxItems",1);

                    if(jsonDatatype!=null) {
                       predicate.add("type", jsonDatatype);
                    }

                } else {

                    if(soln.contains("max") && soln.getLiteral("max").getInt()>1) {
                      predicate.add("maxItems",soln.getLiteral("max").getInt()); 
                    }

                    predicate.add("type", "array");

                    if(jsonDatatype!=null) {
                        predicate.add("items", Json.createObjectBuilder().add("type", jsonDatatype).build());
                    } 

                }
                        

                if(FORMAT_MAP.containsKey(datatype)) {
                    predicate.add("format",FORMAT_MAP.get(datatype));
                }
                
            } else {
                if(soln.contains("shapeRefName")) {
                    String shapeRefName = soln.getLiteral("shapeRefName").getString();

                    
                     if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                             if(soln.contains("min")) predicate.add("minItems",soln.getLiteral("min").getInt());
                             if(soln.contains("max")) {
                                 predicate.add("maxItems",soln.getLiteral("max").getInt());                             
                                logger.info(""+soln.getLiteral("max").getInt());
                             }
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
                if(!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").getString())) {
                    JsonObjectBuilder classDefinition = Json.createObjectBuilder();
                    classDefinition.add("title",soln.getLiteral("classTitle").getString());
                    if(soln.contains("classDescription")) {
                        classDefinition.add("description",soln.getLiteral("classDescription").getString());
                    }
                    classDefinition.add("properties", properties.build());
                    
                    JsonArray reqArray = required.build();
                    
                     if(!reqArray.isEmpty()) {
                        classDefinition.add("required", reqArray);
                    }
                    
                    definitions.add(className, classDefinition.build());
                    properties = Json.createObjectBuilder();
                    required = Json.createArrayBuilder();
                } 
            
            
            
        }
        
        String modelRoot = getModelRoot(modelID);
        
        
        if(modelRoot!=null) {
            JsonObjectBuilder modelProperties = Json.createObjectBuilder();
            modelProperties.add("$ref", "#/definitions/"+SplitIRI.localname(modelRoot));
            return createModelSchemaWithRoot(schema, modelProperties, definitions);
        }
        
        return createDefaultModelSchema(schema, definitions);
        
    }
    
    private static String createDefaultModelSchema(JsonObjectBuilder schema, JsonObjectBuilder definitions) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("definitions", definitions.build());
        
        return jsonObjectToPrettyString(schema.build());
}
    
    private static String createModelSchemaWithRoot(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonObjectBuilder definitions) {
        
        schema.add("$schema", "http://json-schema.org/draft-04/schema#");
        schema.add("type","object");
        schema.add("allOf", Json.createArrayBuilder().add(properties.build()).build());
        schema.add("definitions", definitions.build());
        
        return jsonObjectToPrettyString(schema.build());
}
    
    
    
}
