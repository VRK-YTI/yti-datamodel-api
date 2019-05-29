/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.XMLSchemaBuilder;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.util.SplitIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

@Service
public class XMLSchemaWriter {

    private static final Logger logger = LoggerFactory.getLogger(XMLSchemaWriter.class.getName());

    private static final Map<String, String> DATATYPE_MAP =
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

    private final EndpointServices endpointServices;
    private final GraphManager graphManager;

    XMLSchemaWriter(EndpointServices endpointServices,
                    GraphManager graphManager) {
        this.endpointServices = endpointServices;
        this.graphManager = graphManager;
    }

    public String newClassSchema(String classID,
                                 String lang) {
        logger.info("Creating schema");
        XMLSchemaBuilder xml = new XMLSchemaBuilder();

        String className = SplitIRI.localname(classID);
        String localClassName = null;
        String classDescription = null;
        String classTitle = null;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectClass =
            "SELECT ?localClassName ?type ?label ?description "
                + "WHERE { "
                + "GRAPH ?resourceID { "
                + "?resourceID a ?type . "
                + "?resourceID ?resourceLabel ?label . "
                + "VALUES ?resourceLabel { rdfs:label sh:name }"
                + "FILTER (langMatches(lang(?label),?lang))"
                + "OPTIONAL { ?resourceID iow:localName ?localClassName . } "
                + "OPTIONAL { ?resourceID ?resourceComment ?description . "
                + "VALUES ?resourceComment { rdfs:comment sh:description }"
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "} "
                + "} ";

        pss.setIri("resourceID", classID);
        if (lang != null) pss.setLiteral("lang", lang);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(selectClass);

        boolean classMetadata = false;

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {
            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.debug("Resource results is null");
                return null;
            }

            while (results.hasNext()) {

                QuerySolution soln = results.nextSolution();

                classTitle = soln.getLiteral("label").getString();

                if (soln.contains("description")) {
                    classDescription = soln.getLiteral("description").getString();
                }

                if (soln.contains("localClassName")) {
                    localClassName = soln.getLiteral("localClassName").getString();
                }

                String sType = soln.getResource("type").getLocalName();

                if (sType.equals("Class") || sType.equals("Shape") || sType.equals("NodeShape")) {
                    classMetadata = true;
                }

            }
        }

        Element complexType = xml.newComplexType((localClassName != null && localClassName.length() > 0 ? LDHelper.removeInvalidCharacters(localClassName) : className) + "Type", classID);
        Element cdocumentation = xml.newDocumentation(complexType);
        xml.appendElementValue(cdocumentation, "dcterms:title", classTitle);
        if (classDescription != null) {
            xml.appendElementValue(cdocumentation, "dcterms:description", classDescription);
        }

        if (classMetadata) {

            String selectResources =
                "SELECT ?predicate ?id ?predicateName ?label ?description ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
                    + "WHERE { "
                    + "GRAPH ?resourceID {"
                    + "?resourceID sh:property ?property . "
                    + "?property sh:order ?index . "
                    + "?property sh:path ?predicate . "
                    + "OPTIONAL { ?property iow:localName ?id . }"
                    + "?property sh:name ?label . "
                    + "FILTER (langMatches(lang(?label),?lang))"
                    + "OPTIONAL { ?property sh:description ?description . "
                    + "FILTER (langMatches(lang(?description),?lang))"
                    + "}"
                    + "OPTIONAL { ?property sh:datatype ?datatype . }"
                    + "OPTIONAL { ?property sh:node ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                    + "OPTIONAL { ?property sh:minCount ?min . }"
                    + "OPTIONAL { ?property sh:maxCount ?max . }"
                    + "OPTIONAL { ?property sh:pattern ?pattern . }"
                    + "OPTIONAL { ?property sh:minLength ?minLength . }"
                    + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                    + "BIND(afn:localname(?predicate) as ?predicateName)"
                    + "}"
                    + "} ORDER BY ?index ";

            pss.setCommandText(selectResources);
            if (lang != null) pss.setLiteral("lang", lang);

            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

                ResultSet results = qexec.execSelect();

                if (results.hasNext()) {

                    Element seq = xml.newSequence(complexType);

                    while (results.hasNext()) {

                        QuerySolution soln = results.nextSolution();
                        String predicateName = soln.getLiteral("predicateName").getString();

                        if (soln.contains("id")) {
                            predicateName = soln.getLiteral("id").getString();
                        }

                        String predicate = soln.getResource("predicate").getURI();

                        String title = soln.getLiteral("label").getString();

                        Element newElement = xml.newSimpleElement(seq, predicateName, predicate);
                        Element documentation = xml.newDocumentation(newElement);

                        xml.appendElementValue(documentation, "dcterms:title", title);

                        if (soln.contains("min") && soln.getLiteral("min").getInt() > 0) {
                            int min = soln.getLiteral("min").getInt();
                            newElement.setAttribute("minOccurs", "" + min);
                        } else {
                            newElement.setAttribute("minOccurs", "0");
                        }

                        if (soln.contains("description")) {
                            String description = soln.getLiteral("description").getString();
                            xml.appendElementValue(documentation, "dcterms:description", description);
                        }

                        if (soln.contains("datatype")) {
                            String datatype = soln.getResource("datatype").toString();
                            newElement.setAttribute("type", DATATYPE_MAP.get(datatype));
                        }
                
                /*

                <xs:complexType name="langStringType">
                <xs:simpleContent>
                    <xs:extension base="xs:string">
                        <xs:attribute ref="xml:lang" use="optional"/>
                    </xs:extension>
                </xs:simpleContent>
                </xs:complexType>

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

                        if (soln.contains("max") && soln.getLiteral("max").getInt() > 0) {
                            int max = soln.getLiteral("max").getInt();
                            newElement.setAttribute("maxOccurs", "" + max);
                        } else {
                            newElement.setAttribute("maxOccurs", "unbounded");
                        }

                        /* If shape contains pattern or other type of restriction */

                        if (soln.contains("pattern") || soln.contains("maxLength") || soln.contains("minLength")) {
                            Element simpleType = xml.newSimpleType(predicateName + "Type");

                            if (soln.contains("pattern")) {
                                Element restriction = xml.newStringRestriction(simpleType);
                                xml.appendElementValueAttribute(restriction, "xs:maxInclusive", soln.getLiteral("pattern").toString());
                            } else {
                                Element restriction = xml.newIntRestriction(simpleType);
                                if (soln.contains("maxLength")) {
                                    xml.appendElementValueAttribute(restriction, "xs:maxInclusive", "" + soln.getLiteral("maxLength").getInt());
                                }
                                if (soln.contains("minLength")) {
                                    xml.appendElementValueAttribute(restriction, "xs:minInclusive", "" + soln.getLiteral("minLength").getInt());
                                }
                            }
                            newElement.setAttribute("type", predicateName + "Type");
                        }

                        if (soln.contains("shapeRefName")) {
                            String shapeRef = soln.getLiteral("shapeRefName").toString();
                            newElement.setAttribute("type", shapeRef + "Type");
                        }

                    }
                }
            }

        }

        return xml.toString();
    }

    public String newModelSchema(String modelID,
                                 String lang) {

        logger.info("Building XML Schema from " + modelID);

        XMLSchemaBuilder xml = new XMLSchemaBuilder();

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
        if (lang != null) pss.setLiteral("lang", lang);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(selectClass);

        Element documentation = xml.newDocumentation(xml.getRoot());

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.info("No model found:" + modelID);
                return null;
            }

            while (results.hasNext()) {

                QuerySolution soln = results.nextSolution();
                String title = soln.getLiteral("label").getString();

                if (soln.contains("description")) {
                    String description = soln.getLiteral("description").getString();
                    xml.appendElementValue(documentation, "dcterms:description", description);
                }

                xml.appendElementValue(documentation, "dcterms:title", title);

                Date modified = graphManager.lastModified(modelID);
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

                if (modified != null) {
                    String dateModified = format.format(modified);
                    xml.appendElementValue(documentation, "dcterms:modified", dateModified);
                }

            }

        }
        /* Get classes from library */
        pss = new ParameterizedSparqlString();

        String selectResources =
            "SELECT ?resource ?targetClass ?className ?localClassName ?classTitle ?classDescription ?classDeactivated ?property ?propertyDeactivated ?valueList ?schemeList ?predicate ?id ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource sh:name ?classTitle . "
                + "FILTER (langMatches(lang(?classTitle),?lang))"
                + "OPTIONAL { ?resource sh:deactivated ?classDeactivated . }"
                + "OPTIONAL { ?resource iow:localName ?localClassName . } "
                + "OPTIONAL { ?resource sh:targetClass ?targetClass . }"
                + "OPTIONAL { ?resource sh:description ?classDescription . "
                + "FILTER (langMatches(lang(?classDescription),?lang))"
                + "}"
                + "BIND(afn:localname(?resource) as ?className)"
                + "OPTIONAL{"
                + "?resource sh:property ?property . "
                + "?property sh:order ?index . "
                + "?property sh:path ?predicate . "
                + "OPTIONAL { ?property iow:localName ?id . }"
                + "?property sh:name ?title . "
                + "FILTER (langMatches(lang(?title),?lang))"
                + "OPTIONAL { ?property sh:description ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:deactivated ?propertyDeactivated . }"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:node ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLenght ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "OPTIONAL { ?property sh:in ?valueList . } "
                + "OPTIONAL { ?property dcam:memberOf ?schemeList . } "
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}"
                + "}"
                + "ORDER BY ?resource ?index";

        pss.setIri("modelPartGraph", modelID + "#HasPartGraph");
        if (lang != null) pss.setLiteral("lang", lang);
        pss.setCommandText(selectResources);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();
            ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);

            if (pResults.hasNext()) {

                boolean firstRun = true;
                Element complexType = null;
                Element seq = null;
                String previousPredicateID = null;

                while (pResults.hasNext()) {
                    QuerySolution soln = pResults.nextSolution();

                    if (!soln.contains("className")) return null;

                    if (!soln.contains("classDeactivated") || (soln.contains("classDeactivated") && !soln.getLiteral("classDeactivated").getBoolean())) {

                        String classID = soln.getResource("resource").getURI();
                        String className = soln.getLiteral("className").getString();
                        String localClassName = soln.contains("localClassName") ? soln.getLiteral("localClassName").getString() : null;

                        if (firstRun) {

                            if (soln.contains("targetClass")) {
                                complexType = xml.newComplexType((localClassName != null && localClassName.length() > 0 ? LDHelper.removeInvalidCharacters(localClassName) : className) + "Type", soln.getResource("targetClass").toString());
                            } else {
                                complexType = xml.newComplexType((localClassName != null && localClassName.length() > 0 ? LDHelper.removeInvalidCharacters(localClassName) : className) + "Type", soln.getResource("resource").toString());
                            }

                            Element classDoc = xml.newDocumentation(complexType);
                            xml.appendElementValue(classDoc, "dcterms:title", soln.getLiteral("classTitle").getString());

                            seq = xml.newSequence(complexType);

                            if (soln.contains("description")) {
                                String description = soln.getLiteral("description").getString();
                                xml.appendElementValue(classDoc, "dcterms:description", description);
                            }

                            firstRun = false;
                        }

                        if (soln.contains("property") && (!soln.contains("propertyDeactivated") || (soln.contains("propertyDeactivated") && !soln.getLiteral("propertyDeactivated").getBoolean()))) {

                            String predicate = soln.getResource("predicate").getURI();
                            String property = soln.getResource("property").getURI();
                            String predicateName = soln.getLiteral("predicateName").getString();

                            if (previousPredicateID != null && property.equals(previousPredicateID)) {
                                logger.warn("Problems with duplicate values in " + className + " " + predicateName);
                            } else {

                                previousPredicateID = property;

                                if (soln.contains("id")) {
                                    predicateName = soln.getLiteral("id").getString();
                                }

                                String title = soln.getLiteral("title").getString();

                                Element newElement = xml.newSimpleElement(seq, predicateName, predicate);
                                documentation = xml.newDocumentation(newElement);

                                xml.appendElementValue(documentation, "dcterms:title", title);

                                if (soln.contains("min") && soln.getLiteral("min").getInt() > 0) {
                                    int min = soln.getLiteral("min").getInt();
                                    newElement.setAttribute("minOccurs", "" + min);
                                } else {
                                    newElement.setAttribute("minOccurs", "0");
                                }

                                if (soln.contains("description")) {
                                    String description = soln.getLiteral("description").getString();
                                    xml.appendElementValue(documentation, "dcterms:description", description);
                                }

                                if (soln.contains("datatype")) {
                                    String datatype = soln.getResource("datatype").toString();
                                    newElement.setAttribute("type", DATATYPE_MAP.get(datatype));
                                }

                                if (soln.contains("max") && soln.getLiteral("max").getInt() > 0) {
                                    int max = soln.getLiteral("max").getInt();
                                    newElement.setAttribute("maxOccurs", "" + max);
                                } else {
                                    newElement.setAttribute("maxOccurs", "unbounded");
                                }

                                /* If shape contains pattern or other type of restriction */

                                if (soln.contains("pattern") || soln.contains("maxLength") || soln.contains("minLength")) {
                                    Element simpleType = xml.newSimpleType(predicateName + "Type");

                                    if (soln.contains("pattern")) {
                                        Element restriction = xml.newStringRestriction(simpleType);
                                        xml.appendElementValueAttribute(restriction, "xs:maxInclusive", soln.getLiteral("pattern").toString());
                                    } else {
                                        Element restriction = xml.newIntRestriction(simpleType);
                                        if (soln.contains("maxLength")) {
                                            xml.appendElementValueAttribute(restriction, "xs:maxInclusive", "" + soln.getLiteral("maxLength").getInt());
                                        }
                                        if (soln.contains("minLength")) {
                                            xml.appendElementValueAttribute(restriction, "xs:minInclusive", "" + soln.getLiteral("minLength").getInt());
                                        }
                                    }
                                    newElement.setAttribute("type", predicateName + "Type");
                                }

                                if (soln.contains("shapeRefName")) {
                                    String shapeRef = soln.getLiteral("shapeRefName").toString();
                                    newElement.setAttribute("type", shapeRef + "Type");
                                }
                            }
                
                            /*   
                                if(soln.contains("valueList")) {
                                    JsonArray valueList = getValueList(soln.getResource("resource").toString(),soln.getResource("property").toString());
                                    if(valueList!=null) {
                                        predicate.add("enum",valueList);    
                                    }
                                } else if(soln.contains("schemeList")) {
                                    JsonArray schemeList = getSchemeValueList(soln.getResource("schemeList").toString());
                                    if(schemeList!=null) {
                                        predicate.add("enum",schemeList);
                                    }
                                }
                            */
                        }

                        /* Check if next result is about the same class */
                        if (!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").getString())) {
                            firstRun = true;
                        }

                    }

                }
            }
        }

        return xml.toString();

    }
        
  
    
    
    /*
    public boolean hasModelRoot(String graphIRI) {

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
    
    
    public String getModelRoot(String graph) {
        
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
