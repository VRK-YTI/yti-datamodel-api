/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ImportManager {

    private final EndpointServices endpointServices;
    private final GraphManager graphManager;

    @Autowired
    ImportManager(EndpointServices endpointServices,
                  GraphManager graphManager) {
        this.endpointServices = endpointServices;
        this.graphManager = graphManager;
    }

    /**
     * Creates separate resource graph from existing export graph that is imported with importModel API
     *
     * @param graph ID of the graph
     * @param map   Prefix map used in the model
     */
    public void createResourceGraphs(String graph,
                                     Map<String, String> map) {

        Literal timestamp = LDHelper.getDateTimeLiteral();

        graphManager.deleteResourceGraphs(graph);

        String query
            = " INSERT { "
            + "GRAPH ?hasPartGraph { "
            + " ?graph dcterms:hasPart ?resource . "
            + "}"
            + "GRAPH ?graph { "
            + " ?graph owl:versionInfo ?draft . } "
            + "GRAPH ?resource { "
            + " ?resource dcterms:modified ?date . "
            + " ?resource rdfs:isDefinedBy ?graph . "
            + " ?resource dcterms:subject ?subject . "
            + " ?subject ?sp ?so . "
            + " ?resource sh:property ?propertyID . "
            + " ?propertyID sh:path ?predicate . }}"
            + "WHERE { "
            + " GRAPH ?graph { "
            + " VALUES ?type { rdfs:Class sh:NodeShape owl:DatatypeProperty owl:ObjectProperty } "
            + " ?resource a ?type . "
            + " OPTIONAL { "
            + "  ?resource dcterms:subject ?subject . "
            + "  ?subject ?sp ?so . "
            + " } OPTIONAL { "
            + "  ?resource sh:property ?property . "
            + "  ?property sh:path ?predicate . "
            + "  BIND(UUID() AS ?propertyID) } "
            + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        /* ADD prefix&namespaces from the model*/
        pss.setNsPrefixes(map);

        /* ADD all used in the query to be sure */
        pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        pss.setIri("graph", graph);
        pss.setIri("hasPartGraph", graph + "#HasPartGraph");
        pss.setLiteral("date", timestamp);
        pss.setLiteral("draft", "DRAFT");
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();

        updateResourceGraphs(graph, map);

    }

    /**
     * Removes resource references from imported model graph
     *
     * @param graph ID of the model
     */
    private void removeDuplicatesFromModel(String graph) {

        String query
            = "DELETE { "
            + " GRAPH ?graph { "
            + "?graph owl:imports ?import . "
            + "?resource ?p ?o . "
            + "?resource sh:property ?property . "
            + "?property ?pp ?oo . }}"
            + " WHERE { "
            + "GRAPH ?graph { "
            + "?graph owl:imports ?import .  "
            + "OPTIONAL { VALUES ?type { rdfs:Class sh:NodeShape owl:DatatypeProperty owl:ObjectProperty } "
            + "?resource a ?type . "
            + "?resource ?p ?o . "
            + "OPTIONAL { ?resource sh:property ?property . ?property ?pp ?oo . } } "
            + "}"
            + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        /* ADD all used in the query to be sure */
        pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        pss.setIri("graph", graph);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();
    }

    private void updateResourceGraphs(String model,
                                      Map<String, String> map) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?resource WHERE { GRAPH ?resource { ?resource rdfs:isDefinedBy ?model . }}";

        pss.setIri("model", model);
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setCommandText(selectResources);

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                constructGraphs(model, soln.getResource("resource").toString(), map);
                addIndexNumberToProperties(soln.getResource("resource").toString());
            }

            removeDuplicatesFromModel(model);
        }
    }

    private void addIndexNumberToProperties(String resource) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?property WHERE { GRAPH ?resource { ?resource rdfs:isDefinedBy ?model . ?resource sh:property ?property . ?property sh:path ?predicate . }} ORDER BY ?predicate ";

        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setIri("resource", resource);
        pss.setCommandText(selectResources);

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();
            int id = 1;
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                addIndexToProperty(resource, soln.getResource("property").toString(), id++);
            }
        }
    }

    private void addIndexToProperty(String resource,
                                    String property,
                                    int index) {

        String query
            = " INSERT { "
            + "GRAPH ?resource { "
            + "?resource sh:property ?property . "
            + "?property sh:order ?index . }}"
            + "WHERE { "
            + " GRAPH ?resource { "
            + "?resource sh:property ?property . "
            + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

        pss.setIri("resource", resource);
        pss.setIri("property", property);
        pss.setLiteral("index", index);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();
    }

    private void constructGraphs(String graph,
                                 String resource,
                                 Map<String, String> map) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String query
            = "CONSTRUCT { "
            + "?resource a ?type . "
            + "?resource owl:versionInfo ?draft . "
            + "?resource ?p ?o .  "
            + "?resource sh:property ?uuid . "
            + "?uuid owl:versionInfo ?draft . "
            + "?uuid ?pp ?oo .  "
            + "?resource dcterms:modified ?date . "
            + "?resource rdfs:isDefinedBy ?graph . "
            + " ?resource sh:constraint ?constraint . "
            + " ?constraint a ?constraintType . "
            + " ?constraint rdfs:comment ?constComment . "
            + " ?constraint ?listProperty ?collection . "
            + " ?collection rdfs:label ?label . "
            + "} "
            + " WHERE { "
            + "GRAPH ?graph { "
            + "VALUES ?type { rdfs:Class sh:NodeShape owl:DatatypeProperty owl:ObjectProperty } . "
            + "?resource a ?type . "
            + "?resource ?p ?o . "
            + "FILTER(!isBlank(?o)) "
            + "OPTIONAL { "
            + " ?resource sh:property ?property . "
            + " ?property sh:path ?predicate . "
            + " ?property ?pp ?oo . }"
            + "OPTIONAL { "
            + " ?resource sh:constraint ?constraint . "
            + " ?constraint a ?constraintType . "
            + " ?constraint ?listProperty ?list . "
            + " ?list rdf:rest*/rdf:first ?collection ."
            + " ?collection rdfs:label ?label . "
            + "OPTIONAL { ?constraint rdfs:comment ?constComment . }"
            + "} }  "
            + "GRAPH ?resource { "
            + " OPTIONAL { "
            + "  ?resource sh:property ?uuid . "
            + "  ?uuid sh:path ?predicate ."
            + "}} "
            + "}";

        pss.setIri("graph", graph);
        pss.setIri("resource", resource);
        pss.setLiteral("draft", "DRAFT");
        pss.setNsPrefixes(map);
        pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        pss.setCommandText(query);

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            Model results = qexec.execConstruct();

            try(RDFConnection connection = RDFConnection.connect(endpointServices.getCoreReadWriteAddress())){
                connection.load(resource, results);
            }
        }

    }

    /**
     * Adds namespace and prefix to the model
     *
     * @param model     ID of the model
     * @param namespace New namespace as string
     * @param prefix    New prefix as string
     */
    public void updateModelNamespaceInfo(String model,
                                         String namespace,
                                         String prefix) {

        String query = "INSERT { "
            + "GRAPH ?model { "
            + "?model dcap:preferredXMLNamespaceName ?namespace . "
            + "?model dcap:preferredXMLNamespacePrefix ?prefix . }} "
            + " WHERE { GRAPH ?model { ?model ?p ?o . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", model);
        pss.setLiteral("namespace", namespace);
        pss.setLiteral("prefix", prefix);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();
    }
}
