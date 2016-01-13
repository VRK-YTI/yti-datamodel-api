/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.iri.IRI;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

/**
 *
 * @author malonen
 */
public class GraphManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(GraphManager.class.getName());

    public static boolean testDefaultGraph() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { ?s a sd:Service ; sd:defaultDataset ?d . ?d sd:defaultGraph ?g . ?g dcterms:title ?title . }";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query, "urn:csc:iow:sd");

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean modelStatusRestrictsRemoving(IRI graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { {"
                + "GRAPH ?graph { "
                + "VALUES ?status { \"Draft\" \"Recommendation\" } "
                + "?graph owl:versionInfo ?status . }"
                + "} UNION { "
                + "GRAPH ?graph { "
                + "?graph dcterms:hasPart ?resource . }"
                + "GRAPH ?resource { "
                + "?resource rdfs:isDefinedBy ?graph . "
                + "VALUES ?status { \"Draft\" \"Recommendation\" } "
                + "?resource owl:versionInfo ?status  . "
                + "}"
                + "}}";

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

    public static boolean isExistingGraph(IRI graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s ?p ?o }}";
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

    public static void createDefaultGraph() {

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());

        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGraphInputStream(), RDFLanguages.JSONLD);

        accessor.putModel("urn:csc:iow:sd", m);

    }

    public static String buildRemoveModelQuery(String model) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?graph WHERE { GRAPH ?model { ?model dcterms:hasPart ?graph . } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . }}";

        pss.setIri("model", model);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        String newQuery = "DROP SILENT GRAPH <" + model + ">; ";

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            newQuery += "DROP SILENT GRAPH <" + soln.getResource("graph").toString() + ">; ";
        }

        return newQuery;

    }

    public static Map<String, String> getNamespaceMap() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources
                = "SELECT ?namespace ?prefix WHERE { "
                + "GRAPH ?graph { "
               // + " ?graph a owl:Ontology .  "
                + " ?graph dcap:preferredXMLNamespaceName ?namespace . "
                + " ?graph dcap:preferredXMLNamespacePrefix ?prefix . "
                + "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        Map namespaceMap = new HashMap<String, String>();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
        }

        return namespaceMap;

    }

    public static void removeModel(IRI id) {

        String query = buildRemoveModelQuery(id.toString());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.info("Removing model from " + id);
        logger.info(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());

        try {
            qexec.execute();
        } catch (UpdateException ex) {
            logger.log(Level.WARNING, ex.toString());
        }
    }

    public static void removeGraph(IRI id) {

        String query = "DROP GRAPH ?graph ;";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.log(Level.WARNING, "Removing graph " + id);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());

        try {
            qexec.execute();
        } catch (UpdateException ex) {
            logger.log(Level.WARNING, ex.toString());
        }
    }

    public static void deleteResourceGraphs(String model) {

        String query = "DELETE { GRAPH ?graph { ?s ?p ?o . } } WHERE { GRAPH ?graph { ?s ?p ?o . ?graph rdfs:isDefinedBy ?model . } }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setIri("model", model);

        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

        /* OPTIONALLY
        
         UpdateRequest request = UpdateFactory.create() ;
         request.add("DROP ALL")
         UpdateAction.execute(request, graphStore) ;
        
         */
    }

    public static void deleteGraphs() {

        String query = "DROP ALL";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);

        logger.log(Level.WARNING, pss.toString() + " from " + services.getCoreSparqlUpdateAddress());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void renameID(IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?graph { ?graph dcterms:hasPart ?oldID }}"
                + " INSERT { GRAPH ?graph { ?graph dcterms:hasPart ?newID }}"
                + " WHERE { GRAPH ?graph { ?graph dcterms:hasPart ?oldID }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID", oldID);
        pss.setIri("newID", newID);
        pss.setCommandText(query);

        logger.log(Level.WARNING, "Renaming " + oldID + " to " + newID);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void insertNewGraphReferenceToModel(IRI graph, IRI model) {

        String timestamp = SafeDateFormat.fmt().format(new Date());

        String query
                = " INSERT { GRAPH ?model { ?model dcterms:hasPart ?graph } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . ?graph dcterms:created ?timestamp . }} "
                + " WHERE { GRAPH ?graph {}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);

        logger.log(Level.WARNING, pss.toString() + " " + services.getCoreSparqlUpdateAddress());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void insertExistingGraphReferenceToModel(IRI graph, IRI model) {

      // TODO: ADD MODIFIED DATE TO MODEL
        //   String timestamp = fmt.format(new Date());
        String query
                = " INSERT { GRAPH ?model { ?model dcterms:hasPart ?graph }} "
                + " WHERE { GRAPH ?graph { ?graph a ?type . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        // pss.setLiteral("date", timestamp);
        pss.setCommandText(query);

        logger.log(Level.WARNING, pss.toString() + " " + services.getCoreSparqlUpdateAddress());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void deleteGraphReferenceFromModel(IRI graph, IRI model) {

        String query
                = " DELETE { GRAPH ?model { ?model dcterms:hasPart ?graph }} "
                + " WHERE { GRAPH ?graph { ?graph a ?type . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

}
