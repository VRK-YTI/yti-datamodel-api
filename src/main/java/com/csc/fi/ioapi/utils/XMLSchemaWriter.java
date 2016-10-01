/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;


import com.csc.fi.ioapi.config.EndpointServices;
import java.io.File;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
import org.w3c.dom.Element;

/**
 *
 * @author malonen
 */
public class XMLSchemaWriter {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(XMLSchemaWriter.class.getName());
    
    public static final Map<String, String> DATATYPE_MAP = 
    Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("http://www.w3.org/2001/XMLSchema#int", "xs:int");
        put("http://www.w3.org/2001/XMLSchema#integer", "xs:integer");
        put("http://www.w3.org/2001/XMLSchema#long", "xs:long");
        put("http://www.w3.org/2001/XMLSchema#float", "xs:float");
        put("http://www.w3.org/2001/XMLSchema#double", "xs:double");
        put("http://www.w3.org/2001/XMLSchema#decimal", "xs:decimal");
        put("http://www.w3.org/2001/XMLSchema#boolean", "xs:boolean");
        put("http://www.w3.org/2001/XMLSchema#date", "xs:date");
        put("http://www.w3.org/2001/XMLSchema#dateTime", "xs:dateTime");
        put("http://www.w3.org/2001/XMLSchema#time", "xs:time");
        put("http://www.w3.org/2001/XMLSchema#gYear", "xs:gYear");
        put("http://www.w3.org/2001/XMLSchema#gMonth", "xs:gMont");
        put("http://www.w3.org/2001/XMLSchema#gDay", "xs:gDay");
        put("http://www.w3.org/2001/XMLSchema#string", "xs:string");
        put("http://www.w3.org/2001/XMLSchema#anyUri", "xs:anyUri");
        put("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", "xs:string");
        put("http://www.w3.org/2000/01/rdf-schema#Literal", "xs:string");
        put("http://www.w3.org/2001/XMLSchema#anyUri", "xs:anyUri");
    }});
       
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
        XMLSchemaBuilder xml = new XMLSchemaBuilder();
        logger.info(xml.toString());   
       
        String className = SplitIRI.localname(classID);
        
        Element complexType = xml.newComplexType(className);
        
        logger.info(xml.toString());     
        
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
                // newClassType.setAnnotation(createAnnotation(description,lang));
            }
            
         //   logger.info(soln.getResource("type").getLocalName());
            String sType = soln.getResource("type").getLocalName();
            
            if(sType.equals("Class") || sType.equals("Shape")) {
                classMetadata = true;
            }
            
        }

       
        if(classMetadata) {
        
         logger.info("querying class");
            
        String selectResources = 
                "SELECT ?predicate ?id ?predicateName ?label ?description ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
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
                + "OPTIONAL { ?property sh:valueShape ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLength ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}";
        
        
        pss.setCommandText(selectResources);
        if(lang!=null) pss.setLiteral("lang",lang);

        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();
        
        
        if(!results.hasNext()) return null;
        
        Element seq = xml.newSequence(complexType);
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String predicateName = soln.getLiteral("predicateName").getString();
            
            logger.info(predicateName);
            
            if(soln.contains("id")) {
                predicateName = soln.getLiteral("id").getString();
            }
            
            String title = soln.getLiteral("label").getString();
            
            Element newElement = xml.newSimpleElement(seq, predicateName);
            Element documentation = xml.newDocumentation(newElement);
            
            xml.appendElementValue(documentation, "dcterms:title", title);
            
            if(soln.contains("min") ) {
                int min = soln.getLiteral("min").getInt();
                if(min>0) {
                    newElement.setAttribute("minOccurs", ""+min);
                }
            } 
            
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                xml.appendElementValue(documentation, "dcterms:description", description);
            }
            
            if(soln.contains("datatype")) {
                String datatype = soln.getResource("datatype").toString();
                newElement.setAttribute("type", DATATYPE_MAP.get(datatype));
            }
            
            /* 
            http://examples.oreilly.com/9780596002527/creating-simple-types.html
            <xs:attribute name="lang" type="xs:language"/> OR
            
              <xs:simpleType name="supportedLanguages">
    <xs:restriction base="xs:language">
      <xs:enumeration value="en"/>
      <xs:enumeration value="es"/>
    </xs:restriction>
  </xs:simpleType>
            
              <xs:attribute name="lang" type="supportedLanguages"/>
            
  <xs:element name="title">
    <xs:complexType>
      <xs:simpleContent>
        <xs:extension base="string255">
          <xs:attribute ref="lang"/>
        </xs:extension>
      </xs:simpleContent>
    </xs:complexType>
  </xs:element>
            
            */
            
            if(soln.contains("max") && soln.getLiteral("max").getInt()>0) {
                int max = soln.getLiteral("max").getInt();
                newElement.setAttribute("maxOccurs", ""+max);
            }

            /* If shape contains pattern or other type of restriction */
            
            if(soln.contains("pattern") || soln.contains("maxLength") || soln.contains("minLength"))  {
                    Element simpleType = xml.newSimpleType(predicateName+"Type");
                    
                    logger.info(predicateName+"Type");
                    
                    if(soln.contains("pattern")) {
                        Element restriction = xml.newStringRestriction(simpleType);
                         xml.appendElementValueAttribute(restriction, "xs:maxInclusive", soln.getLiteral("pattern").toString());
                    } else {
                        Element restriction = xml.newIntRestriction(simpleType);
                           if(soln.contains("maxLength")) {
                              xml.appendElementValueAttribute(restriction, "xs:maxInclusive", ""+soln.getLiteral("maxLength").getInt());
                           }
                           if(soln.contains("minLength"))  {
                              xml.appendElementValueAttribute(restriction, "xs:minInclusive", ""+soln.getLiteral("minLength").getInt());
                           }
                    }
                    newElement.setAttribute("type", predicateName+"Type");
                }
                 
                if(soln.contains("shapeRefName")) {
                    String shapeRef = soln.getLiteral("shapeRefName").toString();
                    newElement.setAttribute("type", shapeRef);
                }
                
        }     
        
        }
      
        return xml.toString();
    }
    
    
    
    /*
     public static String newModelSchema(String modelID, String lang) { 
    
        Schema schema = new Schema("urn:uuid:test");
       
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
        
        QueryExecution qexec =  QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.toString());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        while (results.hasNext()) {
            
            QuerySolution soln = results.nextSolution();
            String title = soln.getLiteral("label").getString();
            if(soln.contains("description")) {
                String description = soln.getLiteral("description").getString();
                schema.setAnnotation(createAnnotation(description,lang));
            }
            
            if(!modelID.endsWith("/") || !modelID.endsWith("#")) 
                schema.add("@id",modelID+"#");
            else 
                schema.add("@id",modelID);
            
            
            schema.add("title", title);
            
             Date modified = GraphManager.lastModified(modelID);
             SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
             
                if(modified!=null) {
                    String dateModified = format.format(modified);
                    schema.add("modified",dateModified);
                }
            
            
        }
        
        String modelRoot = getModelRoot(modelID);

        if(modelRoot!=null) {
            
        }
        
        return schema.toString();
        
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
    
    */

    
    
}
