/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import org.apache.jena.util.SplitIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import fi.vm.yti.datamodel.api.utils.LDHelper;

@Service
public class OpenAPIWriter {

    private static final Logger logger = LoggerFactory.getLogger(OpenAPIWriter.class.getName());
    private static final Map<String, String> DATATYPE_MAP =
        Collections.unmodifiableMap(new HashMap<>() {{
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
    private static final Map<String, String> FORMAT_MAP =
        Collections.unmodifiableMap(new HashMap<>() {{
            put("http://www.w3.org/2001/XMLSchema#dateTime", "date-time");
            put("http://www.w3.org/2001/XMLSchema#date", "date");
            put("http://www.w3.org/2001/XMLSchema#time", "time");
            put("http://www.w3.org/2001/XMLSchema#anyURI", "uri");
        }});
    private final EndpointServices endpointServices;
    private final JsonWriterFactory jsonWriterFactory;
    private final GraphManager graphManager;

    OpenAPIWriter(EndpointServices endpointServices,
                  JsonWriterFactory jsonWriterFactory,
                  GraphManager graphManager) {
        this.endpointServices = endpointServices;
        this.jsonWriterFactory = jsonWriterFactory;
        this.graphManager = graphManager;
    }

    public String jsonObjectToPrettyString(JsonObject object) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = jsonWriterFactory.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
    }

    public JsonArray getSchemeValueList(String schemeID) {
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

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getSchemesSparqlAddress(), pss.toString())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) return null;

            while (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln.contains("value")) {
                    builder.add(soln.getLiteral("value").getString());
                }
            }

        }

        return builder.build();
    }

    public JsonArray getValueList(String classID,
                                  String propertyID) {
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

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) return null;

            while (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln.contains("value")) {
                    builder.add(soln.getLiteral("value").getString());
                }
            }
        }

        return builder.build();
    }

    public Map<String, Object> getClassDefinitions(String modelID,
                                                   String lang,
                                                   String resourceID) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectResources =
            "SELECT " + (resourceID != null ? "" : "?resource") + " ?targetClass ?className ?path ?classTitle ?classDeactivated ?classDescription ?minProperties ?maxProperties ?property ?propertyDeactivated ?valueList ?schemeList ?predicate ?id ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern ?idBoolean ?example "
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource a ?resourceType . "
                + "VALUES ?resourceType { rdfs:Class sh:Shape sh:NodeShape }"
                + "OPTIONAL { ?resource sh:name ?classTitle . "
                + "OPTIONAL { ?resource httpv:absolutePath ?path . }"
                + "FILTER (langMatches(lang(?classTitle),?lang)) }"
                + "OPTIONAL { ?resource sh:deactivated ?classDeactivated . }"
                + "OPTIONAL { ?resource iow:minProperties ?minProperties . }"
                + "OPTIONAL { ?resource iow:maxProperties ?maxProperties . }"
                + "OPTIONAL { ?resource sh:targetClass ?targetClass . }"
                + "OPTIONAL { ?resource sh:description ?classDescription . "
                + "FILTER (langMatches(lang(?classDescription),?lang))"
                + "}"
                + "BIND(afn:localname(?resource) as ?className)"
                + "OPTIONAL {"
                + "?resource sh:property ?property . "
                + "?property sh:order ?index . "
                + "?property sh:path ?predicate . "
                + "OPTIONAL { ?property iow:localName ?id . }"
                + "OPTIONAL {?property sh:name ?title . "
                + "FILTER (langMatches(lang(?title),?lang))}"
                + "OPTIONAL { ?property sh:description ?description . "
                + "FILTER (langMatches(lang(?description),?lang))"
                + "}"
                + "OPTIONAL { ?property sh:deactivated ?propertyDeactivated . }"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:node ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLength ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "OPTIONAL { ?property skos:example ?example . }"
                + "OPTIONAL { ?property sh:in ?valueList . } "
                + "OPTIONAL { ?property dcam:memberOf ?schemeList . } "
                + "OPTIONAL { ?property iow:isResourceIdentifier ?idBoolean . }"
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}"
                + "}"
                + "ORDER BY " + (resourceID != null ? "" : "?resource") + " ?index ?property";

        pss.setIri("modelPartGraph", modelID + "#HasPartGraph");

        if (resourceID != null && !LDHelper.isInvalidIRI(resourceID)) {
            pss.setIri("resource", resourceID);
        }

        if (lang == null) {
            lang = "fi";
        }

        pss.setLiteral("lang", lang);

        pss.setCommandText(selectResources);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();
            ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);

            if (!pResults.hasNext()) {
                return null;
            }

            JsonObjectBuilder paths = Json.createObjectBuilder();
            JsonArrayBuilder tags = Json.createArrayBuilder();
            JsonObjectBuilder definitions = Json.createObjectBuilder();
            JsonObjectBuilder properties = Json.createObjectBuilder();

            JsonObjectBuilder predicate = Json.createObjectBuilder();

            HashSet<String> exampleSet = new HashSet<String>();
            HashSet<String> pathSet = new HashSet<String>();
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

                if (!soln.contains("className")) {
                    return null;
                }

                if (!soln.contains("classDeactivated") || (soln.contains("classDeactivated") && !soln.getLiteral("classDeactivated").getBoolean())) {

                    className = soln.getLiteral("className").getString();
                    String classId = resourceID != null ? resourceID : soln.getResource("resource").getURI();

                    if (soln.contains("property") && (!soln.contains("propertyDeactivated") || (soln.contains("propertyDeactivated") && !soln.getLiteral("propertyDeactivated").getBoolean()))) {

                        /* First run per predicate */

                        if (pIndex == 1) {

                            predicateID = soln.getResource("predicate").toString();

                            // predicate.add("@id", predicateID);

                            predicateName = soln.getLiteral("predicateName").getString();

                            if (soln.contains("id")) {
                                predicateName = soln.getLiteral("id").getString();
                            }

                            if (soln.contains("title")) {
                                String title = soln.getLiteral("title").getString();
                                predicate.add("title", title);
                            }

                            if (soln.contains("min")) {
                                int min = soln.getLiteral("min").getInt();
                                if (min > 0) {
                                    requiredPredicates.add(predicateName);
                                }
                            }

                            if (soln.contains("description")) {
                                String description = soln.getLiteral("description").getString();
                                predicate.add("description", description);
                            }

                            if (soln.contains("valueList")) {
                                JsonArray valueList = getValueList(classId, soln.getResource("property").toString());
                                if (valueList != null) {
                                    predicate.add("enum", valueList);
                                }
                            } else if (soln.contains("schemeList")) {
                                JsonArray schemeList = getSchemeValueList(soln.getResource("schemeList").toString());
                                if (schemeList != null) {
                                    predicate.add("enum", schemeList);
                                }
                            }

                            if (soln.contains("datatype")) {

                                String datatype = soln.getResource("datatype").toString();

                                String jsonDatatype = DATATYPE_MAP.get(datatype);

                                if (soln.contains("maxLength")) {
                                    predicate.add("maxLength", soln.getLiteral("maxLength").getInt());
                                }

                                if (soln.contains("minLength")) {
                                    predicate.add("minLength", soln.getLiteral("minLength").getInt());
                                }

                                if (soln.contains("pattern")) {
                                    predicate.add("pattern", soln.getLiteral("pattern").getString());
                                }

                                if (soln.contains("max") && soln.getLiteral("max").getInt() <= 1) {

                                    // predicate.add("maxItems",1);

                                    if (jsonDatatype != null) {
                                        if (jsonDatatype.equals("langString")) {
                                            predicate.add("type", "object");
                                            predicate.add("$ref", "#/components/schemas/langString");
                                        } else {
                                            predicate.add("type", jsonDatatype);
                                        }
                                    }

                                } else {

                                    if (soln.contains("max") && soln.getLiteral("max").getInt() > 1) {
                                        predicate.add("maxItems", soln.getLiteral("max").getInt());
                                    }

                                    if (soln.contains("min") && soln.getLiteral("min").getInt() > 0) {
                                        predicate.add("minItems", soln.getLiteral("min").getInt());
                                    }

                                    predicate.add("type", "array");

                                    arrayType = true;

                                    if (jsonDatatype != null) {

                                        if (jsonDatatype.equals("langString")) {
                                            typeObject.add("type", "object");
                                            typeObject.add("$ref", "#/components/schemas/langString");
                                        } else {
                                            typeObject.add("type", jsonDatatype);
                                        }

                                    }

                                }

                                if (FORMAT_MAP.containsKey(datatype)) {
                                    predicate.add("format", FORMAT_MAP.get(datatype));
                                }

                            } else {
                                if (soln.contains("shapeRefName")) {

                                    //predicate.add("@type", "@id");

                                    String shapeRefName = soln.getLiteral("shapeRefName").getString();

                                    if (!soln.contains("max") || soln.getLiteral("max").getInt() > 1) {
                                        if (soln.contains("min")) {
                                            predicate.add("minItems", soln.getLiteral("min").getInt());
                                        }
                                        if (soln.contains("max")) {
                                            predicate.add("maxItems", soln.getLiteral("max").getInt());

                                        }
                                        predicate.add("type", "array");

                                        predicate.add("items", Json.createObjectBuilder().add("type", "object").add("$ref", "#/components/schemas/" + shapeRefName).build());
                                    } else {
                                        /* Not required by Open API spec ? */
                                        // predicate.add("type", "object");
                                        predicate.add("$ref", "#/components/schemas/" + shapeRefName);
                                    }
                                }
                            }

                        }

                        /* Every run per predicate*/

                        if (soln.contains("example")) {
                            String example = soln.getLiteral("example").getString();
                            exampleSet.add(example);
                        }

                        if (soln.contains("path")) {
                            String path = soln.getLiteral("path").getString();
                            pathSet.add(path);
                        }

                        if (pResults.hasNext() && className.equals(pResults.peek().getLiteral("className").getString()) && (pResults.peek().contains("predicate") && predicateID.equals(pResults.peek().getResource("predicate").toString()))) {

                            pIndex += 1;

                        } else {

                            /* Last run per class */

                            if (!exampleSet.isEmpty()) {

                                Iterator<String> i = exampleSet.iterator();

                                while (i.hasNext()) {
                                    String ex = i.next();
                                    exampleList.add(ex);
                                }

                                predicate.add("example", exampleList.build());

                            }

                            if (arrayType) {
                                predicate.add("items", typeObject.build());
                            }

                            properties.add(predicateName, predicate.build());

                            predicate = Json.createObjectBuilder();
                            typeObject = Json.createObjectBuilder();
                            arrayType = false;
                            pIndex = 1;
                            exampleSet = new HashSet<String>();
                            exampleList = Json.createArrayBuilder();
                        }
                    }

                    /* If not build props and requires */
                    if (!pResults.hasNext() || !className.equals(pResults.peek().getLiteral("className").getString())) {
                        predicate = Json.createObjectBuilder();
                        JsonObjectBuilder classDefinition = Json.createObjectBuilder();

                        if (soln.contains("classTitle")) {
                            classDefinition.add("title", soln.getLiteral("classTitle").getString());
                        }
                        classDefinition.add("type", "object");

                        JsonObjectBuilder uriDocs = Json.createObjectBuilder();
                        uriDocs.add("url", classId);
                        uriDocs.add("description", "Class identifier");
                        classDefinition.add("externalDocs", uriDocs.build());

                        /*
                        if (soln.contains("targetClass")) {
                            classDefinition.add("@id", soln.getResource("targetClass").toString());
                        } else {
                            classDefinition.add("@id", soln.getResource("resource").toString());
                        }*/
                        if (soln.contains("classDescription")) {
                            classDefinition.add("description", soln.getLiteral("classDescription").getString());
                        }
                        if (soln.contains("minProperties")) {
                            classDefinition.add("minProperties", soln.getLiteral("minProperties").getInt());
                        }

                        if (soln.contains("maxProperties")) {
                            classDefinition.add("maxProperties", soln.getLiteral("maxProperties").getInt());
                        }

                        JsonObject classProps = properties.build();

                        if (!classProps.isEmpty()) {
                            classDefinition.add("properties", classProps);
                        }

                        if (!pathSet.isEmpty()) {

                            JsonObjectBuilder tagObject = Json.createObjectBuilder();
                            tagObject.add("name", className);

                                 /*
                                // TAG description object if needed?
                                JsonObjectBuilder tagDocs = Json.createObjectBuilder();
                                tagDocs.add("url",classId);
                                tagDocs.add("description","Documentation");
                                tagObject.add("externalDocs",tagDocs.build());
                                */

                            tags.add(tagObject.build());

                            Iterator<String> i = pathSet.iterator();

                            while (i.hasNext()) {

                                JsonArrayBuilder paramList = Json.createArrayBuilder();
                                String pathString = i.next();
                                if (!pathString.startsWith("/")) pathString = "/" + pathString;
                                JsonObjectBuilder pathObject = Json.createObjectBuilder();

                                // Parse query parameters from path string
                                UriComponents bld = UriComponentsBuilder.fromUriString(pathString).build();
                                MultiValueMap<String, String> queryParameters = bld.getQueryParams();
                                pathString = bld.getPath();
                                queryParameters.forEach((k, v) -> {
                                    JsonObjectBuilder paramObject = Json.createObjectBuilder();
                                    JsonObjectBuilder schemaObject = Json.createObjectBuilder();
                                    schemaObject.add("type", "string");
                                    if (!v.isEmpty() && v.get(0) != null) schemaObject.add("example", v.get(0));
                                    paramObject.add("name", k);
                                    paramObject.add("in", "query");
                                    paramObject.add("schema", schemaObject);
                                    paramList.add(paramObject);
                                });

                                // Parse path parameters from path string
                                Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(pathString);
                                while (m.find()) {
                                    JsonObjectBuilder paramObject = Json.createObjectBuilder();
                                    JsonObjectBuilder schemaObject = Json.createObjectBuilder();
                                    schemaObject.add("type", "string");
                                    paramObject.add("name", m.group(1));
                                    paramObject.add("in", "path");
                                    paramObject.add("required", true);
                                    paramObject.add("schema", schemaObject);
                                    paramList.add(paramObject);
                                }

                                JsonArray parameters = paramList.build();
                                JsonObject invalidParameterObject = null;
                                if (!parameters.isEmpty()) {
                                    pathObject.add("parameters", parameters);
                                    JsonObjectBuilder invalidparam = Json.createObjectBuilder();
                                    invalidparam.add("description", "Invalid parameter");
                                    invalidParameterObject = invalidparam.build();
                                }

                                /* GET API */
                                JsonObjectBuilder getObject = Json.createObjectBuilder();
                                getObject.add("tags", Json.createArrayBuilder().add(className).build());
                                JsonObjectBuilder responsesObject = Json.createObjectBuilder();
                                JsonObjectBuilder successObject = Json.createObjectBuilder();
                                JsonObjectBuilder contentTypeObject = Json.createObjectBuilder();
                                JsonObjectBuilder contentObject = Json.createObjectBuilder();
                                JsonObjectBuilder schemaObject = Json.createObjectBuilder();
                                schemaObject.add("$ref", "#/components/schemas/" + className);
                                contentTypeObject.add("schema", schemaObject.build());
                                contentObject.add("application/json", contentTypeObject.build());
                                successObject.add("content", contentObject.build());
                                successObject.add("description", "Successfull operation");
                                responsesObject.add("200", successObject.build());
                                if (invalidParameterObject != null) responsesObject.add("400", invalidParameterObject);
                                JsonObject successResponse = responsesObject.build();
                                getObject.add("responses", successResponse);
                                getObject.add("summary", "Get " + className);

                                pathObject.add("get", getObject.build());

                                /* POST API */
                                JsonObjectBuilder requestBodyObject = Json.createObjectBuilder();
                                JsonObjectBuilder requestContentObject = Json.createObjectBuilder();
                                JsonObjectBuilder requestContentTypeObject = Json.createObjectBuilder();
                                JsonObjectBuilder requestSchemaObject = Json.createObjectBuilder();
                                requestSchemaObject.add("$ref", "#/components/schemas/" + className);

                                JsonObjectBuilder postObject = Json.createObjectBuilder();
                                postObject.add("tags", Json.createArrayBuilder().add(className).build());
                                JsonObjectBuilder postResponsesObject = Json.createObjectBuilder();
                                JsonObjectBuilder postSuccessObject = Json.createObjectBuilder();
                                JsonObjectBuilder postContentTypeObject = Json.createObjectBuilder();
                                JsonObjectBuilder postContentObject = Json.createObjectBuilder();

                                requestSchemaObject.add("$ref", "#/components/schemas/" + className);
                                requestContentTypeObject.add("schema", requestSchemaObject.build());
                                requestContentObject.add("application/json", requestContentTypeObject.build());
                                requestBodyObject.add("content", requestContentObject.build());

                                postContentObject.add("application/json", Json.createObjectBuilder().build());
                                postSuccessObject.add("content", postContentObject.build());
                                postSuccessObject.add("description", "Successfull operation");
                                postResponsesObject.add("200", postSuccessObject.build());
                                if (invalidParameterObject != null)
                                    postResponsesObject.add("400", invalidParameterObject);

                                postObject.add("requestBody", requestBodyObject.build());
                                postObject.add("responses", postResponsesObject.build());
                                postObject.add("summary", "Post " + className);
                                postObject.add("description", "POST is used to create new " + className + "-resource without a URI identifier or updating resource using URI in path or query parameter. In practice you often dont have to implement both POST and PUT methods. Use PUT if you need idempotent behaviour and to clearly separate creation of the resource with the known URI.");

                                pathObject.add("post", postObject.build());

                                /* PUT API. If path parameters used */
                                JsonObjectBuilder putRequestBodyObject = Json.createObjectBuilder();
                                JsonObjectBuilder putRequestContentObject = Json.createObjectBuilder();
                                JsonObjectBuilder putRequestContentTypeObject = Json.createObjectBuilder();
                                JsonObjectBuilder putRequestSchemaObject = Json.createObjectBuilder();

                                JsonObjectBuilder putObject = Json.createObjectBuilder();
                                putObject.add("tags", Json.createArrayBuilder().add(className).build());
                                JsonObjectBuilder putResponsesObject = Json.createObjectBuilder();
                                JsonObjectBuilder putSuccessObject = Json.createObjectBuilder();
                                JsonObjectBuilder putContentTypeObject = Json.createObjectBuilder();
                                JsonObjectBuilder putContentObject = Json.createObjectBuilder();

                                putRequestSchemaObject.add("$ref", "#/components/schemas/" + className);
                                putRequestContentTypeObject.add("schema", putRequestSchemaObject.build());
                                putRequestContentObject.add("application/json", putRequestContentTypeObject.build());
                                putRequestBodyObject.add("content", putRequestContentObject.build());

                                putContentObject.add("application/json", Json.createObjectBuilder().build());
                                putSuccessObject.add("content", putContentObject.build());
                                putSuccessObject.add("description", "Successfull operation");
                                putResponsesObject.add("200", putSuccessObject.build());
                                if (invalidParameterObject != null)
                                    putResponsesObject.add("400", invalidParameterObject);

                                putObject.add("requestBody", putRequestBodyObject.build());
                                putObject.add("responses", putResponsesObject.build());
                                putObject.add("summary", "Put " + className);
                                putObject.add("description", "PUT is used when you create or update " + className + "-resource using URI in path or query parameter. PUT is defined to be idempotent, which means that if you PUT an object twice, second request is silently ignored and 200 (OK) is automatically returned.");

                                pathObject.add("put", putObject.build());

                                /* DELETE API */
                                JsonObjectBuilder deleteObject = Json.createObjectBuilder();
                                deleteObject.add("summary", "Delete " + className);
                                deleteObject.add("tags", Json.createArrayBuilder().add(className).build());
                                deleteObject.add("responses", successResponse);

                                pathObject.add("delete", deleteObject.build());

                                /* ADD ALL APIs to PATH */
                                paths.add(pathString, pathObject.build());

                            }
                        }

                        pathSet = new HashSet<String>();

                        JsonArrayBuilder required = Json.createArrayBuilder();

                        Iterator<String> ri = requiredPredicates.iterator();

                        while (ri.hasNext()) {
                            String ex = ri.next();
                            required.add(ex);
                        }

                        JsonArray reqArray = required.build();

                        if (!reqArray.isEmpty()) {
                            classDefinition.add("required", reqArray);
                        }

                        definitions.add(className, classDefinition.build());
                        properties = Json.createObjectBuilder();
                        requiredPredicates = new HashSet<String>();
                    }
                }
            }

            Map<String, Object> defs = new HashMap<String, Object>() {
                {
                    put("definitions", definitions);
                    put("paths", paths);
                    put("tags", tags);
                }
            };

            return defs;
        }
    }

    public String newOpenApiStub(String modelID,
                                 String lang) {

        JsonObjectBuilder schema = Json.createObjectBuilder();
        JsonObjectBuilder infoObject = Json.createObjectBuilder();
        JsonObjectBuilder externalDocs = Json.createObjectBuilder();
        JsonArrayBuilder serverArray = Json.createArrayBuilder();
        JsonObjectBuilder serverObject = Json.createObjectBuilder();

        serverObject.add("url", "https://api.example.com/v1");
        serverObject.add("description", "Example server description");
        serverArray.add(serverObject.build());

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

        if (lang==null) {
            lang = "fi";
        }

        pss.setLiteral("lang", lang);

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(selectClass);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.debug("No results from model: " + modelID);
                return null;
            }

            while (results.hasNext()) {

                QuerySolution soln = results.nextSolution();
                String title = soln.getLiteral("label").getString();

                logger.info("Building Open API spesification from " + title);

                if (soln.contains("description")) {
                    String description = soln.getLiteral("description").getString();
                    infoObject.add("description", (lang.equals("fi") ? "Automaattisesti generoitu Open API rajapintakuvaus. Huom! Rajapintakuvauksen voi tuottaa useammalla eri kielellä. Tietomallin kuvaus kielellä fi: " : "Automatically generated Open API specification. Notice that this specification can be generated in multiple languages! Datamodel description in " + lang + ": ") + description);
                }
                
                /*
                if (!modelID.endsWith("/") || !modelID.endsWith("#"))
                    schema.add("@id", modelID + "#");
                else
                    schema.add("@id", modelID);
                */

                infoObject.add("title", title);

                infoObject.add("version", "0.01");

                /*
                Date modified = graphManager.lastModified(modelID);
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

                if (modified != null) {
                    String dateModified = format.format(modified);
                    schema.add("modified", dateModified);
                }*/

            }

            externalDocs.add("url", modelID);
            externalDocs.add("description", lang.equals("fi") ? "Rajapinnan tietomalli" : "Datamodel for the API");

            schema.add("openapi", "3.0.0");
            schema.add("info", infoObject.build());
            schema.add("externalDocs", externalDocs.build());
            schema.add("servers", serverArray.build());

            Map<String, Object> defs = getClassDefinitions(modelID, lang, null);

            JsonArray tagArr = ((JsonArrayBuilder) defs.get("tags")).build();
            if (!tagArr.isEmpty()) {
                schema.add("tags", tagArr);
            }

            return createDefaultOpenAPI(schema, (JsonObjectBuilder) defs.get("definitions"), (JsonObjectBuilder) defs.get("paths"));
        }
    }

    public String newOpenApiStubFromClass(String classID,
                                          String lang) {

        JsonObjectBuilder schema = Json.createObjectBuilder();
        JsonObjectBuilder infoObject = Json.createObjectBuilder();
        JsonObjectBuilder externalDocs = Json.createObjectBuilder();
        JsonArrayBuilder serverArray = Json.createArrayBuilder();
        JsonObjectBuilder serverObject = Json.createObjectBuilder();

        serverObject.add("url", "https://api.example.com/v1");
        serverObject.add("description", "Example server description");
        serverArray.add(serverObject.build());

        String className = SplitIRI.localname(classID);
        infoObject.add("title", className);
        infoObject.add("description", "Automatically generated Open API skeleton from " + className + " in " + lang + ". Notice that this spec can be exported in different languages. Full spec including all classes can be exported under datamodel export.");
        infoObject.add("version", "0.01");

        externalDocs.add("url", classID);
        externalDocs.add("description", lang.equals("fi") ? "Rajapinnan tietomalli" : "Datamodel for the API");

        schema.add("openapi", "3.0.0");
        schema.add("info", infoObject.build());
        schema.add("externalDocs", externalDocs.build());
        schema.add("servers", serverArray.build());

        Map<String, Object> defs = getClassDefinitions(LDHelper.guessNamespaceFromResourceURI(classID), lang, classID);

        if (defs != null) {
            JsonArray tagArr = ((JsonArrayBuilder) defs.get("tags")).build();
            if (!tagArr.isEmpty()) {
                schema.add("tags", tagArr);
            }
        }

        return createDefaultOpenAPI(schema, defs != null ? (JsonObjectBuilder) defs.get("definitions") : null, defs != null ? (JsonObjectBuilder) defs.get("paths") : null);
    }

    public JsonObject getLangStringObject() {

        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObjectBuilder add = Json.createObjectBuilder();
        builder.add("type", "object");
        builder.add("title", "Multilingual string");
        builder.add("description", "Object type for localized strings");
        builder.add("additionalProperties", add.add("type", "string").build());
        return builder.build();
    }

    private String createDefaultOpenAPI(JsonObjectBuilder root,
                                        JsonObjectBuilder definitions,
                                        JsonObjectBuilder paths) {

        if (definitions != null) {
            definitions.add("langString", getLangStringObject());

            JsonObjectBuilder components = Json.createObjectBuilder();
            JsonObject defObject = definitions.build();

            if (!defObject.isEmpty()) {
                components.add("schemas", defObject);

                JsonObject pathObject = paths.build();
                if (!pathObject.isEmpty()) {
                    root.add("paths", pathObject);
                }

                root.add("components", components.build());
            }
        }

        return jsonObjectToPrettyString(root.build());
    }

}
