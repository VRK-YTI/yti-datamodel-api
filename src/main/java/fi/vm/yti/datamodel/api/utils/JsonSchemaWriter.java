/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.EndpointServices;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import org.apache.jena.query.Query;
import org.apache.jena.util.SplitIRI;

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
                put("http://www.w3.org/2001/XMLSchema#anyURI", "string");
                put("http://www.w3.org/2001/XMLSchema#hexBinary", "string");
                put("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", "langString");
                put("http://www.w3.org/2000/01/rdf-schema#Literal", "string");
            }});

    public static final Map<String, String> FORMAT_MAP =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("http://www.w3.org/2001/XMLSchema#dateTime", "date-time");
                put("http://www.w3.org/2001/XMLSchema#anyURI", "uri");
            }});

    private static final Map<String, Boolean> config;
    private static final JsonWriterFactory factory;
    static {
        config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        factory = Json.createWriterFactory(config);
    }


    public static String newResourceSchema(String classID, String lang) {

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
                schema.add("description", description);
            }

            schema.add("id",classID+".jschema");
            schema.add("title", title);

            schema.add("@id",classID);

            String sType = soln.getResource("type").getLocalName();

            if(sType.equals("Class") || sType.equals("Shape")) {
                classMetadata = true;
            }

        }

        JsonObjectBuilder properties = Json.createObjectBuilder();


        if(classMetadata) {

            String selectResources =
                    "SELECT ?predicate ?id ?property ?valueList ?schemeList ?predicateName ?label ?datatype ?shapeRef ?min ?max ?minLength ?maxLenght ?pattern ?idBoolean "
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
                            + "OPTIONAL { ?property sh:in ?valueList . } "
                            + "OPTIONAL { ?property dcam:memberOf ?schemeList . } "
                            + "OPTIONAL { ?property iow:isResourceIdentifier ?idBoolean . }"
                            + "BIND(afn:localname(?predicate) as ?predicateName)"
                            + "}"
                            + "}";


            pss.setCommandText(selectResources);
            pss.setIri("resourceID", classID);
            if(lang!=null) pss.setLiteral("lang",lang);

            qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

            results = qexec.execSelect();


            if(!results.hasNext()) return null;

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                //String predicateID = soln.getResource("predicate").getString();
                String predicateName = soln.getLiteral("predicateName").getString();

                if(soln.contains("id")) {
                    predicateName = soln.getLiteral("id").getString();
                }

                String title = soln.getLiteral("label").getString();

                JsonObjectBuilder predicate = Json.createObjectBuilder();

                predicate.add("title", title);

                if(soln.contains("min") ) {
                    int min = soln.getLiteral("min").getInt();
                    if(min>0) {
                        required.add(predicateName);
                    }
                }

                if(soln.contains("description")) {
                    String description = soln.getLiteral("description").getString();
                    predicate.add("description", description);
                }

                if(soln.contains("predicate")) {
                    String predicateID = soln.getResource("predicate").toString();
                    predicate.add("@id", predicateID);
                }

                if(soln.contains("valueList")) {
                    JsonArray valueList = getValueList(classID,soln.getResource("property").toString());
                    if(valueList!=null) {
                        predicate.add("enum",valueList);
                    }
                } else if(soln.contains("schemeList")) {
                    JsonArray schemeList = getSchemeValueList(soln.getResource("schemeList").toString());
                    if(schemeList!=null) {
                        predicate.add("enum",schemeList);
                    }
                }

                if(soln.contains("datatype")) {
                    String datatype = soln.getResource("datatype").toString();

                    if(soln.contains("idBoolean")) {
                        Boolean isId = soln.getLiteral("idBoolean").getBoolean();
                        if(isId) {
                            predicate.add("@type", "@id");
                        } else predicate.add("@type", datatype);
                    } else {
                        predicate.add("@type", datatype);
                    }


                    String jsonDatatype = DATATYPE_MAP.get(datatype);

                    if(soln.contains("min") && soln.getLiteral("min").getInt()>0) {
                        predicate.add("minItems",soln.getLiteral("min").getInt());
                    }


                    if(soln.contains("max") && soln.getLiteral("max").getInt()<=1) {

                        predicate.add("maxItems",1);

                        if(jsonDatatype!=null) {

                            if(jsonDatatype.equals("langString")) {
                                predicate.add("type","object");
                                predicate.add("$ref","#/definitions/langString");
                            }
                            else
                                predicate.add("type", jsonDatatype);
                        }

                    } else {

                        if(soln.contains("max") && soln.getLiteral("max").getInt()>1) {
                            predicate.add("maxItems",soln.getLiteral("max").getInt());
                        }

                        predicate.add("type", "array");

                        logger.info(jsonDatatype);

                        if(jsonDatatype!=null) {

                            JsonObjectBuilder typeObject = Json.createObjectBuilder();

                            if(jsonDatatype.equals("langString")) {
                                typeObject.add("type", "object");
                                typeObject.add("$ref","#/definitions/langString");
                            } else {
                                typeObject.add("type",jsonDatatype);
                            }

                            predicate.add("items", typeObject.build());


                        }

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

                    if(FORMAT_MAP.containsKey(datatype)) {
                        predicate.add("format",FORMAT_MAP.get(datatype));
                    }
                } else {
                    if(soln.contains("shapeRef")) {
                        String shapeRef = soln.getResource("shapeRef").toString();
                        predicate.add("@type", "@id");
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

    public static String jsonObjectToPrettyString(JsonObject object) {

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

    public static JsonArray getSchemeValueList(String schemeID) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectList =
                "SELECT ?value "
                        + "WHERE { "
                        + "GRAPH ?scheme { "
                        + "?code dcterms:identifier ?value . "
                        + "} "
                        + "} ORDER BY ?value";

        pss.setIri("scheme", schemeID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectList);

        //logger.info(""+pss);

        QueryExecution qexec =  QueryExecutionFactory.sparqlService(services.getSchemesSparqlAddress(), pss.toString());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;

        while (results.hasNext()) {
            QuerySolution soln = results.next();
            if(soln.contains("value")) {
                builder.add(soln.getLiteral("value").getString());
            }
        }

        return builder.build();
    }

    public static JsonArray getValueList(String classID, String propertyID) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectList =
                "SELECT ?value "
                        + "WHERE { "
                        + "GRAPH ?resource { "
                        + "?resource sh:property ?property . "
                        + "?property sh:in/rdf:rest*/rdf:first ?value"
                        + "} "
                        + "} ";

        pss.setIri("resource", classID);
        pss.setIri("property", propertyID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectList);

        //  logger.info(""+pss);

        QueryExecution qexec =  QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.toString());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;

        while (results.hasNext()) {
            QuerySolution soln = results.next();
            if(soln.contains("value")) {
                builder.add(soln.getLiteral("value").getString());
            }
        }

        return builder.build();
    }


    /*
    Ways to describe codelists, by "type"-list.

        {
        type:[
        {enum:["22PC"], description:"a description for the first enum"},
        {enum:["42GP"], description:"a description for the second enum"},
        {enum:["45GP"], description:"a description for the third enum"},
        {enum:["45UP"], description:"a description for the fourth enum"},
        {enum:["22GP"], description:"a description for the fifth enum"}
        ]
        }

     or by using custom parameters:

        enum:[1,2,3],
        options:[{value:1,descrtiption:"this is one"},{value:2,description:"this is two"}],


    */

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

    public static JsonObject idProperty() {
        JsonObjectBuilder idPredicate = Json.createObjectBuilder();
        idPredicate.add("title", "JSON-LD identifier");
        idPredicate.add("description","This property is reserved for IRI identifiers. It is highly recommended to use @id to uniquely identify object with IRIs. May be omitted if objects are considered to be non unique or blank nodes.");
        idPredicate.add("type", "string");
        idPredicate.add("format","uri");
        return idPredicate.build();
    }

    public static JsonObjectBuilder getClassDefinitions(String modelID, String lang) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectResources =
                "SELECT ?resource ?scopeClass ?className ?classTitle ?classDescription ?property ?valueList ?schemeList ?predicate ?id ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern ?idBoolean ?example "
                        + "WHERE { "
                        + "GRAPH ?modelPartGraph {"
                        + "?model dcterms:hasPart ?resource . "
                        + "}"
                        + "GRAPH ?resource {"
                        + "?resource rdfs:label ?classTitle . "
                        + "FILTER (langMatches(lang(?classTitle),?lang))"
                        + "OPTIONAL { ?resource sh:scopeClass ?scopeClass . }"
                        + "OPTIONAL { ?resource rdfs:comment ?classDescription . "
                        + "FILTER (langMatches(lang(?classDescription),?lang))"
                        + "}"
                        + "?resource sh:property ?property . "
                        + "?property sh:index ?index . "
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
                        + "OPTIONAL { ?property skos:example ?example . }"
                        + "OPTIONAL { ?property sh:in ?valueList . } "
                        + "OPTIONAL { ?property dcam:memberOf ?schemeList . } "
                        + "OPTIONAL { ?property iow:isResourceIdentifier ?idBoolean . }"
                        + "BIND(afn:localname(?predicate) as ?predicateName)"
                        + "}"
                        + "}"
                        + "ORDER BY ?resource ?index ?property";


        pss.setIri("modelPartGraph", modelID+"#HasPartGraph");

        if(lang!=null) {
            pss.setLiteral("lang",lang);
        }

        pss.setCommandText(selectResources);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);

        if(!pResults.hasNext()) {
            return null;
        }

        JsonObjectBuilder definitions = Json.createObjectBuilder();
        JsonObjectBuilder properties = Json.createObjectBuilder();

        JsonObjectBuilder predicate = Json.createObjectBuilder();

        HashSet<String> exampleSet = new HashSet<String>();
        HashSet<String> requiredPredicates = new HashSet<String>();

        JsonArrayBuilder exampleList = Json.createArrayBuilder();
        JsonObjectBuilder typeObject = Json.createObjectBuilder();

        boolean arrayType = false;

        int pIndex = 1;
        String predicateName = null;
        String predicateID = null;
        String className = null;

        while (pResults.hasNext()) {
            QuerySolution soln = pResults.nextSolution();

            if(!soln.contains("className")) {
                return null;
            }

            /* First run per predicate */

            if(pIndex==1) {

                logger.info("First run");

                className = soln.getLiteral("className").getString();

                logger.info("Class:"+className);

                predicateID = soln.getResource("predicate").toString();

                predicate.add("@id", predicateID);

                predicateName = soln.getLiteral("predicateName").getString();

                if(soln.contains("id")) {
                    predicateName = soln.getLiteral("id").getString();
                    logger.info("Predicatename: "+predicateName);
                }

                String title = soln.getLiteral("title").getString();

                // JsonObjectBuilder predicate = Json.createObjectBuilder();

                predicate.add("title", title);
                
                if(soln.contains("min")) {
                    int min = soln.getLiteral("min").getInt();
                    if(min>0) {
                        requiredPredicates.add(predicateName);
                    }
                }

                if(soln.contains("description")) {
                    String description = soln.getLiteral("description").getString();
                    predicate.add("description", description);
                }

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

                if(soln.contains("datatype")) {

                    String datatype = soln.getResource("datatype").toString();

                    if(soln.contains("idBoolean")) {
                        Boolean isId = soln.getLiteral("idBoolean").getBoolean();
                        if(isId) {
                            predicate.add("@type", "@id");
                        } else predicate.add("@type", datatype);
                    } else {
                        predicate.add("@type", datatype);
                    }

                    String jsonDatatype = DATATYPE_MAP.get(datatype);

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

                       // predicate.add("maxItems",1);

                        if(jsonDatatype!=null) {
                            if(jsonDatatype.equals("langString")) {
                                predicate.add("type","object");
                                predicate.add("$ref","#/definitions/langString");
                            } else {
                                predicate.add("type", jsonDatatype);
                            }
                        }

                    } else {

                        if(soln.contains("max") && soln.getLiteral("max").getInt()>1) {
                            predicate.add("maxItems",soln.getLiteral("max").getInt());
                        }

                        if(soln.contains("min") && soln.getLiteral("min").getInt()>0) {
                            predicate.add("minItems",soln.getLiteral("min").getInt());
                        }


                        predicate.add("type", "array");

                        arrayType=true;

                        if(jsonDatatype!=null) {

                            if(jsonDatatype.equals("langString")) {
                                typeObject.add("type", "object");
                                typeObject.add("$ref","#/definitions/langString");
                            } else {
                                typeObject.add("type",jsonDatatype);
                            }

                        }

                    }


                    if(FORMAT_MAP.containsKey(datatype)) {
                        predicate.add("format",FORMAT_MAP.get(datatype));
                    }

                } else {
                    if(soln.contains("shapeRefName")) {

                        predicate.add("@type", "@id");

                        String shapeRefName = soln.getLiteral("shapeRefName").getString();


                        if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                            if(soln.contains("min")) {
                                predicate.add("minItems",soln.getLiteral("min").getInt());
                            }
                            if(soln.contains("max")) {
                                predicate.add("maxItems",soln.getLiteral("max").getInt());

                            }
                            predicate.add("type", "array");

                            predicate.add("items", Json.createObjectBuilder().add("type","object").add("$ref","#/definitions/"+shapeRefName).build());
                        } else {
                            predicate.add("type","object");
                            predicate.add("$ref","#/definitions/"+shapeRefName);
                        }
                    }
                }

            }

            /* Every run per predicate*/

            if(soln.contains("example")) {
                String example = soln.getLiteral("example").getString();
                exampleSet.add(example);
            }

            if(pResults.hasNext() && predicateID.equals(pResults.peek().getResource("predicate").toString()) && className.equals(pResults.peek().getLiteral("className").getString())) {

                pIndex+=1;

            } else {

                /* Last run per class */

                if(!exampleSet.isEmpty()) {

                    Iterator<String> i = exampleSet.iterator();

                    while(i.hasNext()) {
                        String ex = i.next();
                        exampleList.add(ex);
                    }

                    predicate.add("example", exampleList.build());

                }

                if(arrayType) {
                    predicate.add("items", typeObject.build());
                }

                properties.add(predicateName,predicate.build());
                predicate = Json.createObjectBuilder();
                typeObject = Json.createObjectBuilder();
                arrayType = false;
                pIndex=1;
                exampleSet = new HashSet<String>();
                exampleList = Json.createArrayBuilder();
            }


                /* If not build props and requires */
            if(!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").getString())) {
                predicate = Json.createObjectBuilder();
                JsonObjectBuilder classDefinition = Json.createObjectBuilder();
                classDefinition.add("title",soln.getLiteral("classTitle").getString());
                classDefinition.add("type", "object");
                if(soln.contains("scopeClass")) {
                    classDefinition.add("@id",soln.getResource("scopeClass").toString());
                } else {
                    classDefinition.add("@id",soln.getResource("resource").toString());
                }
                if(soln.contains("classDescription")) {
                    classDefinition.add("description",soln.getLiteral("classDescription").getString());
                }
                classDefinition.add("properties", properties.build());

                JsonArrayBuilder required = Json.createArrayBuilder();

                Iterator<String> ri = requiredPredicates.iterator();

                while(ri.hasNext()) {
                    String ex = ri.next();
                    required.add(ex);
                }

                JsonArray reqArray = required.build();

                if(!reqArray.isEmpty()) {
                    classDefinition.add("required", reqArray);
                }

                definitions.add(className, classDefinition.build());
                properties = Json.createObjectBuilder();
                requiredPredicates = new HashSet<String>();
            }

        }

        return definitions;

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

        JsonObjectBuilder definitions = getClassDefinitions(modelID,lang);

        String modelRoot = getModelRoot(modelID);

        if(modelRoot!=null) {
            JsonObjectBuilder modelProperties = Json.createObjectBuilder();
            modelProperties.add("$ref", "#/definitions/"+SplitIRI.localname(modelRoot));
            return createModelSchemaWithRoot(schema, modelProperties, definitions);
        }

        return createDefaultModelSchema(schema, definitions);

    }

    public static JsonObject getLangStringObject() {

        /*
         Regexp for validating language codes ?
         For example:
         "langString":{
            "type":"object",
            "patternProperties":{"^[a-z]{2,3}(?:-[A-Z]{2,3}(?:-[a-zA-Z]{4})?)?$":{"type":"string"}},
            "additionalProperties":false

        }*/

        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObjectBuilder add = Json.createObjectBuilder();
        builder.add("type", "object");
        builder.add("title","Multilingual string");
        builder.add("description","Object type for localized strings");
        builder.add("additionalProperties",add.add("type", "string").build());
        return builder.build();
    }


    public static String newMultilingualModelSchema(String modelID) {

        JsonObjectBuilder schema = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectClass =
                "SELECT ?lang ?title ?description "
                        + "WHERE { "
                        + "GRAPH ?modelID { "
                        + "?modelID rdfs:label ?title . "
                        + "BIND(lang(?title) as ?lang)"
                        + "OPTIONAL { ?modelID rdfs:comment ?description . "
                        + "FILTER(lang(?description)=lang(?title))"
                        + "}"
                        + "} "
                        + "}";

        pss.setIri("modelID", modelID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(selectClass);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;

        JsonObjectBuilder titleObject = Json.createObjectBuilder();
        JsonObjectBuilder descriptionObject = null;

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String lang = soln.getLiteral("lang").getString();
            String title = soln.getLiteral("title").getString();
            titleObject.add(lang, title);
            if(soln.contains("description")) {
                if(descriptionObject==null) descriptionObject = Json.createObjectBuilder();
                String description = soln.getLiteral("description").getString();
                descriptionObject.add(lang, description);
            }
        }

        schema.add("id",modelID+".jschema");
        schema.add("title", titleObject);

        if(descriptionObject!=null)
            schema.add("description", descriptionObject);


        String selectResources =
                "SELECT ?resource ?property ?lang ?className ?classTitle ?classDescription ?predicate ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?propertyLabel ?propertyDescription ?idBoolean "
                        + "WHERE { "
                        + "GRAPH ?modelPartGraph {"
                        + "?model dcterms:hasPart ?resource . "
                        + "}"
                        + "GRAPH ?resource {"
                        + "?resource rdfs:label ?classTitle . "
                        + "BIND(lang(?classTitle) as ?lang)"
                        + "OPTIONAL { ?resource rdfs:comment ?classDescription . "
                        + "FILTER(?lang=lang(?classDescription))"
                        + "}"
                        + "?resource sh:property ?property . "
                        + "?property sh:predicate ?predicate . "
                        + "?property rdfs:label ?propertyLabel . "
                        + "FILTER(?lang=lang(?propertyLabel))"
                        + "BIND(afn:localname(?resource) as ?className)"
                        + "OPTIONAL { ?property rdfs:comment ?propertyDescription . "
                        + "FILTER(?lang=lang(?propertyDescription))"
                        + "}"
                        + "OPTIONAL { ?property sh:datatype ?datatype . }"
                        + "OPTIONAL { ?property sh:valueShape ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                        + "OPTIONAL { ?property sh:minCount ?min . }"
                        + "OPTIONAL { ?property sh:maxCount ?max . }"
                        + "OPTIONAL { ?property iow:isResourceIdentifier ?idBoolean . }"
                        + "BIND(afn:localname(?predicate) as ?predicateName)"
                        + "}"
                        + "} GROUP BY ?resource ?property ?lang ?className ?classTitle ?classDescription ?predicate ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?propertyLabel ?propertyDescription ?idBoolean "
                        + "ORDER BY ?resource ?property ?lang";

        pss.setIri("modelPartGraph", modelID+"#HasPartGraph");
        pss.setCommandText(selectResources);

        qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        results = qexec.execSelect();
        ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);

        if(!pResults.hasNext()) {
            return null;
        }

        JsonObjectBuilder definitions = Json.createObjectBuilder();

        definitions.add("langString", getLangStringObject());

        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();

        String propertyID = null;
        JsonObjectBuilder classTitleObject = Json.createObjectBuilder();
        JsonObjectBuilder classDescriptionObject = Json.createObjectBuilder();
        JsonObjectBuilder propertyTitleObject = Json.createObjectBuilder();
        JsonObjectBuilder propertyDescriptionObject = Json.createObjectBuilder();

        while (pResults.hasNext()) {

            QuerySolution soln = pResults.nextSolution();

            if(!soln.contains("className")) return null;

            propertyID = soln.getResource("property").toString();
            String lang = soln.getLiteral("lang").getString();
            String className = soln.getLiteral("className").getString();
            String predicateName = soln.getLiteral("predicateName").getString();
            String title = soln.getLiteral("propertyLabel").getString();
            String classTitle = soln.getLiteral("classTitle").getString();

            String propertyDescription = null;
            String classDescription = null;

            if(soln.contains("propertyDescription")) {
                propertyDescription = soln.getLiteral("propertyDescription").getString();
            }

            if(soln.contains("classDescription")) {
                classDescription = soln.getLiteral("classDescription").getString();
            }

            JsonObjectBuilder propertyBuilder = Json.createObjectBuilder();
            
            /* Build multilingual objects */
            
            propertyTitleObject.add(lang, title);
            
            if(propertyDescription!=null)
                propertyDescriptionObject.add(lang, propertyDescription);

            classTitleObject.add(lang,classTitle);

            if(classDescription!=null)
                classDescriptionObject.add(lang, classDescription);

            /* If property is iterated the last time build the rest */
            if(pResults.hasNext() && !propertyID.equals(pResults.peek().getResource("property").toString())) {

                propertyBuilder.add("title", propertyTitleObject.build());

                JsonObject propertyDescriptionJSON = propertyDescriptionObject.build();
                if(!propertyDescriptionJSON.isEmpty()) {
                    propertyBuilder.add("description",propertyDescriptionJSON);
                }

                if(soln.contains("min")) {
                    int min = soln.getLiteral("min").getInt();
                    if(min>0) {
                        required.add(predicateName);
                    }
                }

                if(soln.contains("datatype")) {

                    String datatype = soln.getResource("datatype").toString();

                    if(soln.contains("idBoolean")) {
                        Boolean isId = soln.getLiteral("idBoolean").getBoolean();
                        if(isId) {
                            propertyBuilder.add("@type", "@id");
                        } else propertyBuilder.add("@type", datatype);
                    } else {
                        propertyBuilder.add("@type", datatype);
                    }

                    String jsonDatatype = DATATYPE_MAP.get(datatype);


                    if(soln.contains("maxLength"))  {
                        propertyBuilder.add("maxLength",soln.getLiteral("maxLength").getInt());
                    }

                    if(soln.contains("minLength"))  {
                        propertyBuilder.add("minLength",soln.getLiteral("minLength").getInt());
                    }

                    if(soln.contains("pattern"))  {
                        propertyBuilder.add("pattern",soln.getLiteral("pattern").getString());
                    }

                    if(soln.contains("max") && soln.getLiteral("max").getInt()<=1) {

                        // propertyBuilder.add("maxItems",1);

                        if(jsonDatatype!=null) {
                            if(jsonDatatype.equals("langString")) {
                                propertyBuilder.add("type","object");
                                propertyBuilder.add("$ref","#/definitions/langString");
                            }
                            else
                                propertyBuilder.add("type", jsonDatatype);
                        }

                    } else {

                        if(soln.contains("max") && soln.getLiteral("max").getInt()>1) {
                            propertyBuilder.add("maxItems",soln.getLiteral("max").getInt());
                        }

                        if(soln.contains("min") && soln.getLiteral("min").getInt()>0) {
                            propertyBuilder.add("minItems",soln.getLiteral("min").getInt());
                        }

                        propertyBuilder.add("type", "array");

                        if(jsonDatatype!=null) {

                            JsonObjectBuilder typeObject = Json.createObjectBuilder();

                            if(jsonDatatype.equals("langString")) {
                                typeObject.add("type", "object");
                                typeObject.add("$ref","#/definitions/langString");
                            } else {
                                typeObject.add("type",jsonDatatype);
                            }

                            propertyBuilder.add("items", typeObject.build());
                        }

                    }


                    if(FORMAT_MAP.containsKey(datatype)) {
                        propertyBuilder.add("format",FORMAT_MAP.get(datatype));
                    }

                } else {
                    if(soln.contains("shapeRefName")) {

                        propertyBuilder.add("@type", "@id");

                        String shapeRefName = soln.getLiteral("shapeRefName").getString();


                        if(!soln.contains("max") || soln.getLiteral("max").getInt()>1) {
                            if(soln.contains("min")) {
                                propertyBuilder.add("minItems",soln.getLiteral("min").getInt());
                            }
                            if(soln.contains("max")) {
                                propertyBuilder.add("maxItems",soln.getLiteral("max").getInt());
                                logger.info(""+soln.getLiteral("max").getInt());
                            }
                            propertyBuilder.add("type", "array");
                            propertyBuilder.add("items", Json.createObjectBuilder().add("type","object").add("$ref","#/definitions/"+shapeRefName).build());
                        } else {
                            propertyBuilder.add("type","object");
                            propertyBuilder.add("$ref","#/definitions/"+shapeRefName);
                        }
                    }
                }

                properties.add(predicateName,propertyBuilder.build());

                propertyTitleObject = Json.createObjectBuilder();
                propertyDescriptionObject = Json.createObjectBuilder();
                propertyBuilder = Json.createObjectBuilder();


            }


            /* IF the class is iterated last time */

                /* If not build props and requires */
            if(!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").toString())) {
                JsonObjectBuilder classDefinition = Json.createObjectBuilder();
                classDefinition.add("title",classTitleObject.build());
                classDefinition.add("type", "object");
                JsonObject classDescriptionJSON = classDescriptionObject.build();
                if(!classDescriptionJSON.isEmpty()) {
                    classDefinition.add("description",classDescriptionJSON);
                }
                classDefinition.add("properties", properties.build());
                classDefinition.add("required", required.build());
                definitions.add(className, classDefinition.build());
                properties = Json.createObjectBuilder();
                required = Json.createArrayBuilder();

                classTitleObject = Json.createObjectBuilder();
                classDescriptionObject = Json.createObjectBuilder();


            }



        }

        return createV5ModelSchema(schema, definitions);

    }


    private static String createV5ModelSchema(JsonObjectBuilder schema, JsonObjectBuilder definitions) {

        if(definitions==null) return null;

        schema.add("$schema", "http://tietomallit.suomi.fi/api/draft05jsonld.json");

        schema.add("type","object");
        definitions.add("langString", getLangStringObject());
        schema.add("definitions", definitions.build());
        return jsonObjectToPrettyString(schema.build());
    }


    private static String createDefaultModelSchema(JsonObjectBuilder schema, JsonObjectBuilder definitions) {

        if(definitions==null) return null;

        schema.add("$schema", "http://json-schema.org/draft-04/schema#");

        schema.add("type","object");
        definitions.add("langString", getLangStringObject());
        schema.add("definitions", definitions.build());

        return jsonObjectToPrettyString(schema.build());
    }

    private static String createModelSchemaWithRoot(JsonObjectBuilder schema, JsonObjectBuilder properties, JsonObjectBuilder definitions) {

        if(definitions==null) return null;

        schema.add("$schema", "http://json-schema.org/draft-04/schema#");

        schema.add("type","object");
        schema.add("allOf", Json.createArrayBuilder().add(properties.build()).build());
        definitions.add("langString", getLangStringObject());
        schema.add("definitions", definitions.build());

        return jsonObjectToPrettyString(schema.build());
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


}
