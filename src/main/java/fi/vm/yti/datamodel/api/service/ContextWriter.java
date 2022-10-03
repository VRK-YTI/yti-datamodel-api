/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.apache.jena.query.*;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;


@Service
public class ContextWriter {

    private final EndpointServices endpointServices;
    private final JsonSchemaWriter jsonSchemaWriter;

    ContextWriter(EndpointServices endpointServices,
                  JsonSchemaWriter jsonSchemaWriter) {
        this.endpointServices = endpointServices;
        this.jsonSchemaWriter = jsonSchemaWriter;
    }

    /**
     * Creates context from JSONObjectBuilder
     *
     * @param context
     * @return Returns JSON-LD Context
     */
    private String createDefaultContext(JsonObjectBuilder context) {

        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        contextBuilder.add("@context", context.build());

        return jsonSchemaWriter.jsonObjectToPrettyString(contextBuilder.build());
    }

    /**
     * Creates context object from resource od
     *
     * @param classID Id of the class
     * @return Returns generated JSON-LD context from the SHACL spec
     */
    public String newResourceContext(String classID) {

        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources =
            "SELECT ?resource ?type ?localResourceName ?resourceName ?datatype ?targetClass "
                + "WHERE { "
                + "{GRAPH ?resourceID { "
                + "?resourceID a ?type . "
                + "BIND(?resourceID as ?resource)"
                + "BIND(afn:localname(?resourceID) as ?resourceName)"
                + "OPTIONAL { ?resourceID sh:targetClass ?targetClass . }"
                + "OPTIONAL { ?resourceID iow:localName ?localResourceName . } "
                + "OPTIONAL { ?resourceID a owl:DatatypeProperty . ?resourceID rdfs:range ?datatype . }"
                + "}} UNION{ "
                + "GRAPH ?resourceID {"
                + "?resourceID sh:property ?property . "
                + "OPTIONAL { ?property iow:localName ?localResourceName . } "
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "?property sh:path ?predicate . "
                + "BIND(?predicate as ?resource)"
                + "BIND(afn:localname(?predicate) as ?resourceName)"
                + "}}"
                + "}";

        pss.setIri("resourceID", classID);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) return null;

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String resourceURI = soln.contains("targetClass") ? soln.getResource("targetClass").toString() : soln.getResource("resource").toString();
                String resourceName = soln.getLiteral("resourceName").toString();
                String localResourceName = soln.contains("localResourceName") ? LDHelper.removeInvalidCharacters(soln.getLiteral("localResourceName").getString()) : null;

                JsonObjectBuilder resourceObject = Json.createObjectBuilder();

                String type = soln.contains("type") ? soln.getResource("type").getURI() : null;

                if (type != null && (type.equals(RDFS.Class.getURI()) || type.equals(SH.NodeShape.getURI()))) {
                    context.add(localResourceName != null && localResourceName.length() > 0 ? localResourceName : resourceName, resourceURI);
                } else {
                    resourceObject.add("@id", resourceURI);
                    if (soln.contains("datatype")) {
                        resourceObject.add("@type", soln.getResource("datatype").toString());
                    } else {
                        resourceObject.add("@type", "@id");
                    }
                    context.add(localResourceName != null && localResourceName.length() > 0 ? localResourceName : resourceName, resourceObject.build());
                }
            }
            return createDefaultContext(context);
        }
    }

    /**
     * Return JSON-LD context for the model
     *
     * @param modelID Model id
     * @return Generated JSON-LD context from the model
     */
    public String newModelContext(String modelID) {

        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources =
            "SELECT ?resource ?type ?resourceName ?localResourceName ?datatype ?targetClass "
                + "WHERE { {"
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource a ?type . "
                + "OPTIONAL { ?resource sh:targetClass ?targetClass }"
                + "BIND(afn:localname(?resource) as ?resourceName)"
                + "OPTIONAL { ?resource iow:localName ?localResourceName . } "
                + "}"
                + "OPTIONAL {"
                + "GRAPH ?class {"
                + "?class sh:property ?property . "
                + "?property sh:path ?resource . "
                + "?property sh:datatype ?datatype . "
                + "}"
                + "} } UNION {"
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?shapes . "
                + "}"
                + "GRAPH ?shapes {"
                + "?shapes sh:property ?property . "
                + "?property sh:path ?resource . "
                + "BIND(afn:localname(?resource) as ?resourceName)"
                + "OPTIONAL { ?property iow:localName ?localResourceName . } "
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "}"
                + "} }";

        pss.setIri("model", modelID);
        pss.setIri("modelPartGraph", modelID + "#HasPartGraph");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if (!results.hasNext()) return null;

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String resourceURI = soln.getResource("resource").toString();
            String resourceName = soln.getLiteral("resourceName").toString();
            String localResourceName = soln.contains("localResourceName") ? LDHelper.removeInvalidCharacters(soln.getLiteral("localResourceName").getString()) : null;

            if (soln.contains("targetClass")) {
                resourceURI = soln.getResource("targetClass").toString();
            }

            JsonObjectBuilder resourceObject = Json.createObjectBuilder();

            String type = soln.contains("type") ? soln.getResource("type").getURI() : null;

            if (type != null && (type.equals(RDFS.Class.getURI()) || type.equals(SH.NodeShape.getURI()))) {
                context.add(localResourceName != null && localResourceName.length() > 0 ? localResourceName : resourceName, resourceURI);
            } else {
                resourceObject.add("@id", resourceURI);

                if (soln.contains("datatype")) {
                    resourceObject.add("@type", soln.getResource("datatype").toString());
                } else {
                    resourceObject.add("@type", "@id");
                }
                context.add(localResourceName != null && localResourceName.length() > 0 ? localResourceName : resourceName, resourceObject.build());
            }
        }

        return createDefaultContext(context);
    }
}
