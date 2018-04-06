/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import javax.json.Json;
import javax.json.JsonObjectBuilder;


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
     * @param classID Id of the class
     * @return Returns generated JSON-LD context from the SHACL spec
     */
    public String newResourceContext(String classID) {
    
        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = 
                "SELECT ?resource ?resourceName ?datatype "
                + "WHERE { "
                + "{GRAPH ?resourceID { "
                + "?resourceID a sh:Shape . "
                + "?resourceID sh:scopeClass ?scopeClass . "
                + "BIND(?scopeClass as ?resource)"
                + "BIND(afn:localname(?resourceID) as ?resourceName)"
                + "OPTIONAL { ?resourceID a owl:DatatypeProperty . ?resourceID rdfs:range ?datatype . }"
                + "}} UNION "
                + "{GRAPH ?resourceID { "
                + "?resourceID a rdfs:Class . "
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
        
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) return null;

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String predicateID = soln.getResource("resource").toString();
                String predicateName = soln.getLiteral("resourceName").toString();

                JsonObjectBuilder predicate = Json.createObjectBuilder();
                predicate.add("@id", predicateID);

                if (soln.contains("datatype")) {
                    predicate.add("@type", soln.getResource("datatype").toString());
                } else {
               /* FIXME: Too bold? */
                    predicate.add("@type", "@id");
                }

                context.add(predicateName, predicate.build());
            }

            return createDefaultContext(context);
        }
    }

    /**
     * Return JSON-LD context for the model
     * @param modelID Model id
     * @return Generated JSON-LD context from the model
     */
    public String newModelContext(String modelID) {
    
        JsonObjectBuilder context = Json.createObjectBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = 
                "SELECT ?resource ?type ?resourceName ?datatype ?scopeClass "
                + "WHERE { {"
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource a ?type . "
                + "OPTIONAL { ?resource sh:scopeClass ?scopeClass }"
                + "BIND(afn:localname(?resource) as ?resourceName)"
                + "}"
                + "OPTIONAL {"
                + "GRAPH ?class {"
                + "?class sh:property ?property . "
                + "?property sh:predicate ?resource . "
                + "?property sh:datatype ?datatype . "                
                + "}"
                + "} } UNION {"
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?shapes . "
                + "}"
                + "GRAPH ?shapes {"
                + "?shapes sh:property ?property . "
                + "?property sh:predicate ?resource . "
                + "?property dcterms:type ?type . "
                + "BIND(afn:localname(?resource) as ?resourceName)"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "}"
                + "} }";

        pss.setIri("model", modelID);
        pss.setIri("modelPartGraph",modelID+"#HasPartGraph");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        if(!results.hasNext()) return null;
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String predicateID = soln.getResource("resource").toString();
            String predicateName = soln.getLiteral("resourceName").toString();
            
            if(soln.contains("scopeClass")) {
                predicateID = soln.getResource("scopeClass").toString();
            }
            
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
