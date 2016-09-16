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
    
    /* 
    Alternative way to document?
    
        <ccts:Component>
                 <ccts:ComponentType>BBIE</ccts:ComponentType>
                 <ccts:DictionaryEntryName>Winning Party. Rank.
                 Text</ccts:DictionaryEntryName>
                 <ccts:Definition>Indicates the rank obtained in the
                 award.</ccts:Definition>
                 <ccts:Cardinality>0..1</ccts:Cardinality>
                 <ccts:ObjectClass>Winning Party</ccts:ObjectClass>
                 <ccts:PropertyTerm>Rank</ccts:PropertyTerm>
                 <ccts:RepresentationTerm>Text</ccts:RepresentationTerm>
                 <ccts:DataType>Text. Type</ccts:DataType>
     </ccts:Component>
   
    */

       
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
