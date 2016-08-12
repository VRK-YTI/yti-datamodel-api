/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateException;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.iri.IRI;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

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
    
    public static void createExportGraph(String graph) {
        ThreadExecutor.getInstance().pool.execute(new ExportGraphRunnable(graph));
    }
    
    public static void createExportGraphInRunnable(String graph) {
                   
             String queryString = "CONSTRUCT { "
                + "?model <http://purl.org/dc/terms/hasPart> ?resource . "    
                + "?ms ?p ?o . "
                + "?rs ?rp ?ro . "
                + "?resource ?anyp ?anyx . "
                + " } WHERE {"
                + "GRAPH ?modelHasPartGraph { "
                + "?model <http://purl.org/dc/terms/hasPart> ?resource . "
                + " } GRAPH ?model {"
                + "?ms ?p ?o . "
                + "} GRAPH ?resource { "
                + "?rs ?rp ?ro . "
                + "}"
                + "OPTIONAL {GRAPH ?modelPositionGraph {"
                + "?resource ?anyp ?anyx . "
                + "}}"
                + "}"; 
             
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", graph);
        pss.setIri("modelHasPartGraph", graph+"#HasPartGraph");
        pss.setIri("modelPositionGraph", graph+"#PositionGraph");

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        Model exportModel = qexec.execConstruct();
        
        qexec.close();

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        logger.info("Exporting graph "+graph);
        adapter.putModel(graph+"#ExportGraph", exportModel);
          
    }
    
    public static void deleteExportModel(String graph) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);  
        adapter.deleteModel(graph+"#ExportGraph");
    }

    
    public static void updateAllExportGraphs() {
            
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String selectResources = "SELECT ?name WHERE { "
                + "GRAPH <urn:csc:iow:sd> { "
                + "?graph a sd:NamedGraph . "
                + "?graph sd:name ?name . "
                + "}}";
        
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String graphName = soln.getResource("name").toString();
            logger.info(graphName);
            createExportGraph(graphName);
        }

    }
    
    
    public static boolean modelStatusRestrictsRemoving(IRI graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { {"
                + "GRAPH ?graph { "
                + "VALUES ?status { \"Draft\" \"Recommendation\" } "
                + "?graph owl:versionInfo ?status . }"
                + "} UNION { "
                + "GRAPH ?hasPartGraph { "
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
    
public static boolean isExistingPrefix(String prefix) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { {GRAPH ?graph { ?s ?p ?o . }} UNION { ?s a dcterms:Standard . ?s dcap:preferredXMLNamespacePrefix ?prefix . }}";
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(queryString);
        pss.setLiteral("prefix", prefix);
        pss.setIri("graph",ApplicationProperties.getDefaultDomain()+"ns/"+prefix);

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
            logger.info(graphIRI.toDisplayString()+": "+b);
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isExistingServiceGraph(String graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = " ASK { GRAPH <urn:csc:iow:sd> { " +
                " ?service a sd:Service . "+
                " ?service sd:availableGraphs ?graphCollection . "+
                " ?graphCollection a sd:GraphCollection . "+
                " ?graphCollection sd:namedGraph ?graph . "+
                " ?graph sd:name ?graphName . "+
                "}}";
        
        // TODO: FIXME. Graph IRIs dont end in #
        if(graphIRI.endsWith("#")) graphIRI = graphIRI.substring(0,graphIRI.length()-1);
        
        pss.setCommandText(queryString);
        pss.setIri("graphName", graphIRI);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isExistingGraph(String graphIRI) {

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
        String selectResources = "SELECT ?graph WHERE { GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph . } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . }}";

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

    public static void removeModel(IRI id) {

        String query = buildRemoveModelQuery(id.toString());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.info("Removing model from " + id);

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
    
    
        public static void updateModifyDates(String resource) {

        String query =
                  "DELETE { GRAPH ?resource { ?resource dcterms:modified ?oldModDate . } } "
                + "INSERT { GRAPH ?resource { ?resource dcterms:modified ?time . } } "
                + "WHERE { GRAPH ?resource { ?resource dcterms:modified ?oldModDate .  } BIND(now() as ?time) }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("resource", resource);

        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
    
    }

    public static void deleteGraphs() {

        String query = "DROP ALL";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);

        logger.log(Level.WARNING, pss.toString() + " DROPPING ALL FROM CORE/PROV/SKOS SERVICES");

        UpdateRequest queryObj = pss.asUpdate();
        
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
        qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();
        
        qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getUsersSparqlUpdateAddress());
        qexec.execute();
            
    }

    public static void renameID(IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?hasPartGraph { ?graph dcterms:hasPart ?oldID }}"
                + " INSERT { GRAPH ?hasPartGraph { ?graph dcterms:hasPart ?newID }}"
                + " WHERE { GRAPH ?hasPartGraph { ?graph dcterms:hasPart ?oldID }}";

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

    public static void insertNewGraphReferenceToModel(String graph, String model) {

        String timestamp = SafeDateFormat.fmt().format(new Date());

        String query
                = " INSERT { "
                + "GRAPH ?hasPartGraph { "
                + "?model dcterms:hasPart ?graph "
                + "} "
                + "GRAPH ?graph { "
                + "?graph rdfs:isDefinedBy ?model . "
                + "?graph dcterms:created ?timestamp . }} "
                + " WHERE { GRAPH ?graph { ?graph a ?type . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setIri("hasPartGraph", model+"#HasPartGraph");
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void insertExistingGraphReferenceToModel(String graph, String model) {

        String query
                = " INSERT { GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph }} "
                + " WHERE { GRAPH ?graph { ?graph a ?type . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setIri("hasPartGraph", model+"#HasPartGraph");
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    public static void deleteGraphReferenceFromModel(IRI graph, IRI model) {

        String query
                = " DELETE { GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph }} "
                + " WHERE { "
                + "GRAPH ?model { ?model a ?type . }"
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

        public static void addGraphFromServiceToService(String fromGraph, String toGraph, String fromService, String toService) throws NullPointerException {
        
        DatasetAccessor fromAccessor = DatasetAccessorFactory.createHTTP(fromService);
        Model graphModel = fromAccessor.getModel(fromGraph);
        
        if(graphModel==null) {
            throw new NullPointerException();
        }
        
        DatasetAccessor toAccessor = DatasetAccessorFactory.createHTTP(toService);
        toAccessor.add(toGraph, graphModel);
        
    }
    
    
}
