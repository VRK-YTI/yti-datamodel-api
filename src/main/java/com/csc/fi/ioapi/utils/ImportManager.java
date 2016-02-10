/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author malonen
 */
public class ImportManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ImportManager.class.getName());

    public static void createResourceGraphs(String graph, Map<String, String> map) {

        String timestamp = SafeDateFormat.fmt().format(new Date());

        GraphManager.deleteResourceGraphs(graph);

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
                + " ?propertyID sh:predicate ?predicate . }}"
                + "WHERE { "
                + " GRAPH ?graph { "
                + " VALUES ?type { rdfs:Class sh:Shape owl:DatatypeProperty owl:ObjectProperty } "
                + " ?resource a ?type . "
                + " OPTIONAL { "
                + "  ?resource dcterms:subject ?subject . "
                + "  ?subject ?sp ?so . "
                + " } OPTIONAL { "
                + "  ?resource sh:property ?property . "
                + "  ?property sh:predicate ?predicate . "
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
        pss.setIri("hasPartGraph", graph+"#HasPartGraph");
        pss.setLiteral("date", timestamp);
        pss.setLiteral("draft", "Unstable");
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

        updateResourceGraphs(graph, map);

    }

    public static void removeDuplicatesFromModel(String graph) {

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
                + "OPTIONAL { VALUES ?type { rdfs:Class sh:Shape owl:DatatypeProperty owl:ObjectProperty } "
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
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
    }

    public static void updateResourceGraphs(String model, Map<String, String> map) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?resource WHERE { GRAPH ?resource { ?resource rdfs:isDefinedBy ?model . }}";

        pss.setIri("model", model);
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            constructGraphs(model, soln.getResource("resource").toString(), map);
            addIndexNumberToProperties(soln.getResource("resource").toString());
        }

        removeDuplicatesFromModel(model);

    }

    public static void addIndexNumberToProperties(String resource) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?property WHERE { GRAPH ?resource { ?resource rdfs:isDefinedBy ?model . ?resource sh:property ?property . ?property sh:predicate ?predicate . }} ORDER BY ?predicate ";

        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setIri("resource", resource);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        int id = 1;
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            addIndexToProperty(resource, soln.getResource("property").toString(), id++);
        }

    }

    public static void addIndexToProperty(String resource, String property, int index) {

        String query
                = " INSERT { "
                + "GRAPH ?resource { "
                + "?resource sh:property ?property . "
                + "?property sh:index ?index . }}"
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
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void constructGraphs(String graph, String resource, Map<String, String> map) {

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
                + "VALUES ?type { rdfs:Class sh:Shape owl:DatatypeProperty owl:ObjectProperty } . "
                + "?resource a ?type . "
                + "?resource ?p ?o . "
                + "FILTER(!isBlank(?o)) "
                + "OPTIONAL { "
                + " ?resource sh:property ?property . "
                + " ?property sh:predicate ?predicate . "
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
                + "  ?uuid sh:predicate ?predicate ."
                + "}} "
                + "}";

        pss.setIri("graph", graph);
        pss.setIri("resource", resource);
        pss.setLiteral("draft", "Unstable");
        pss.setNsPrefixes(map);
        pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        pss.setCommandText(query);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        Model results = qexec.execConstruct();

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        accessor.add(resource, results);

    }

    public static void updateModelNamespaceInfo(String model, String namespace, String prefix) {

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

        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

}
