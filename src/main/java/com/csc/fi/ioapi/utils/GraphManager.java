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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

/**
 *
 * @author malonen
 */
public class GraphManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(GraphManager.class.getName());
    
    /**
     * TODO: Static lock - should be fixed
     */
    public static ReentrantLock lock = new ReentrantLock();

    /**
     * Returns true if there are some services defined
     * @return boolean
     */
    public static boolean testDefaultGraph() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { ?s a sd:Service ; sd:defaultDataset ?d . ?d sd:defaultGraph ?g . ?g dcterms:title ?title . }";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        Query query = pss.asQuery();
        logger.info("Querying: " + services.getCoreSparqlAddress());
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query, "urn:csc:iow:sd");

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Default graph test failed", ex);
            return false;
        }
    }
    
    /**
     * Runs createExportGraphInRunnable in runnable
     * @param graph
     */
    public static void createExportGraphInRunnable(String graph) {
        //createExportGraphInRunnable(graph);
        ThreadExecutor.pool.execute(new ExportGraphRunnable(graph));
    }
   
    /**
     * Creates export graph by joining all the resources to one graph
     * @param graph model IRI that is used to create export graph
     */
    public static void constructExportGraph(String graph) {
             lock.lock();
             String queryString = "CONSTRUCT { "
                + "?model <http://purl.org/dc/terms/hasPart> ?resource . "    
                + "?ms ?mp ?mo . "
                + "?rs ?rp ?ro . "
                + " } WHERE {"
                + " GRAPH ?model {"
                + "?ms ?mp ?mo . "
                + "} OPTIONAL {"
                + "GRAPH ?modelHasPartGraph { "
                + " ?model <http://purl.org/dc/terms/hasPart> ?resource . "
                + " } GRAPH ?resource { "
                + "?rs ?rp ?ro . "
                + "}"
                + "}}"; 
          
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", graph);
        pss.setIri("modelHasPartGraph", graph+"#HasPartGraph");

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        Model exportModel = qexec.execConstruct();
        
        qexec.close();

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        logger.info("Exporting graph "+graph);
        adapter.putModel(graph+"#ExportGraph", exportModel);
        
        lock.unlock();
          
    }
    
    /**
     * Deleted export graph
     * @param graph IRI of the graph that is going to be deleted
     */
    public static void deleteExportModel(String graph) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);  
        adapter.deleteModel(graph+"#ExportGraph");
    }
    
    /**
     * Creates new graph with owl:Ontology type
     * @param graph IRI of the graph to be created
     */
    public static void createNewOntologyGraph(String graph) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        Model empty = ModelFactory.createDefaultModel();
        empty.setNsPrefixes(LDHelper.PREFIX_MAP);
        empty.add(ResourceFactory.createResource(graph), RDF.type, OWL.Ontology);
        adapter.putModel(graph, empty);
    }
    
    /**
     * Returns graph from core service
     * @param graph IRI of the graph
     * @return Returns graph as Jena model
     */
    public static Model getCoreGraph(String graph){
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        return adapter.getModel(graph);
    }
    
    /**
     * Updates all export graphs
     */
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
            if(!isExistingGraph(graphName+"#ExportGraph")) {
                constructExportGraph(graphName);
            }
        }

    }
    
    /**
     * Test if model status restricts removing of the model
     * @param graphIRI IRI of the graph
     * @return Returns true if model status or resource status is "Draft" or "Recommendation".
     */
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
    
    /**
     * TODO: Check also against known model from lov etc?
     * @param prefix Used prefix of the namespace
     * @return true if prefix is in use by existing model or standard
     */
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
    
    /**
     * Check if Graph is existing
     * @param graphIRI IRI of the graphs
     * @return Returns true if Graph with the given IRI
     */
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
    
        /**
     * Check if Graph is existing
     * @param graphIRI IRI of the graphs
     * @return Returns true if Graph with the given URL String
     */
    public static boolean isExistingGraph(String graphIRI) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s ?p ?o }}";
        pss.setCommandText(queryString);
        pss.setIri("graph", graphIRI);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            logger.info(graphIRI+": "+b);
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
        /**
     * Test if namespace exists as NamedGraph in GraphCollection
     * @param graphIRI IRI of the graph as String
     * @return Returns true if there is a graph in Service description
     */
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
 
    
    /**
     * Check if there exists a Graph that uses the given prefix
     * @param prefix Prefix given to the namespace
     * @return Returns true prefix is in use
     */
    public static boolean isExistingGraphBasedOnPrefix(String prefix) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        String queryString = " ASK { GRAPH ?graph { ?graph a owl:Ontology . ?graph dcap:preferredXMLNamespacePrefix ?prefix . }}";
        pss.setCommandText(queryString);
        pss.setIri("prefix", prefix);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Returns service graph IRI as string with given prefix
     * @param prefix Prefix used in some model
     * @return Returns graph IRI with given prefix
     */
    public static String getServiceGraphNameWithPrefix(String prefix) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources =
                "SELECT ?graph WHERE { "
                + "GRAPH ?graph { "+
                " ?graph a owl:Ontology . "
                + "?graph dcap:preferredXMLNamespacePrefix ?prefix . "+
                "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        pss.setLiteral("prefix",prefix);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        String graphUri = null;
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if(soln.contains("graph")) {
                Resource resType = soln.getResource("graph");
                graphUri = resType.getURI();
            }
        }

        return graphUri;

    }

    /**
     * Initializes Core service with default Graph from static resources file
     */
    public static void createDefaultGraph() {

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());

        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGraphInputStream(), RDFLanguages.JSONLD);

        accessor.putModel("urn:csc:iow:sd", m);

    }

    /**
     * Builds DROP query by querying model resource graphs
     * @param model Model id as IRI String
     * @return Returns remove query as string
     */
    public static String buildRemoveModelQuery(String model) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?graph WHERE { GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph . } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . }}";

        pss.setIri("model", model);
        pss.setIri("hasPartGraph",model+"#HasPartGraph");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        String newQuery = "DROP SILENT GRAPH <" + model + ">; ";
               newQuery += "DROP SILENT GRAPH <" + model + "#HasPartGraph>; ";
               newQuery += "DROP SILENT GRAPH <" + model + "#ExportGraph>; ";
               newQuery += "DROP SILENT GRAPH <" +model + "#PositionGraph>; ";

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            newQuery += "DROP SILENT GRAPH <" + soln.getResource("graph").toString() + ">; ";
        }

        return newQuery;

    }

    /**
     * Removes all graphs related to the model graph
     * @param id IRI of the graph
     */
    public static void removeModel(IRI id) {

        String query = buildRemoveModelQuery(id.toString());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.info("Removing model from " + id);
        logger.info(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        
        /* TODO: remove when resolved JENA-1255 */
        namespaceBugFix(id.toString());
        
        try {
            qexec.execute();
        } catch (UpdateException ex) {
            logger.log(Level.WARNING, ex.toString());
        }
    }
    
    /**
     * TODO: remove when resolved JENA-1255. This removes model graph by putting empty model to the graph.
     * @param id
     */
    public static void namespaceBugFix(String id) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        Model empty = ModelFactory.createDefaultModel();
        adapter.putModel(id, empty);
    }

    /**
     * Tries to remove single graph
     * @param id IRI of the graph to be removed
     */
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

    /**
     * Tries to remove single graph
     * @param id String IRI of the graph to be removed
     */
    public static void removeGraph(String id) {

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
    
    
    /**
     * Tries to Delete contents of the resource graphs linked to the model graph
     * @param model String representing the IRI of an model graph
     */
    public static void deleteResourceGraphs(String model) {

        String query = "DELETE { GRAPH ?graph { ?s ?p ?o . } } WHERE { GRAPH ?graph { ?s ?p ?o . ?graph rdfs:isDefinedBy ?model . } }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setIri("model", model);

        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

        /* OPTIONALLY. Ummm. Not really?
        
         UpdateRequest request = UpdateFactory.create() ;
         request.add("DROP ALL")
         UpdateAction.execute(request, graphStore) ;
        
         */
    }
    
    /**
     * TODO: Remove!? Not in use. Fixed in front?
     * @param model
     */
    public static void deleteExternalGraphReferences(String model) {

        String query = "DELETE { "
                + "GRAPH ?graph { ?any rdfs:label ?label . } } "
                + "WHERE { GRAPH ?graph { "
                + "?graph dcterms:requires ?any . "
                + "?any a dcap:MetadataVocabulary . "
                + "?any rdfs:label ?label . "
                + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", model);

        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
    }
    
    /**
     * Tries to update the dcterms:modified date of an resource
     * @param resource IRI of an Resource as String
     */
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

    /**
     * Deletes all graphs from Core service.
     */
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
    
    
    /**
     * Renames IRI:s in HasPart-graph
     * @param oldID Old id IRI
     * @param newID New id IRI
     */
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
    
    /**
     * Renames Association target IRI:s
     * @param modelID Model IRI
     * @param oldID Old resource IRI
     * @param newID New resource IRI
     */
    public static void updateClassReferencesInModel(IRI modelID, IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?graph { ?any sh:valueShape ?oldID }} "
                + " INSERT { GRAPH ?graph { ?any sh:valueShape ?newID }} "
                + " WHERE { "
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph . } "
                + "GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . ?any sh:valueShape ?oldID}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID", oldID);
        pss.setIri("newID", newID);
        pss.setIri("model", modelID);
        pss.setIri("hasPartGraph", modelID+"#HasPartGraph");
        pss.setCommandText(query);

        logger.info(pss.toString());
        logger.log(Level.WARNING, "Updating references in "+modelID.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }
    
    /**
     * Updates IRI:s in Position-graph
     * @param modelID Model IRI
     * @param oldID Old resource IRI
     * @param newID New resource IRI
     */
    public static void updateReferencesInPositionGraph(IRI modelID, IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?graph { ?oldID ?anyp ?anyo . }} "
                + " INSERT { GRAPH ?graph { ?newID ?anyp ?anyo . }} "
                + " WHERE { "
                + "GRAPH ?graph { ?oldID ?anyp ?anyo . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID", oldID);
        pss.setIri("newID", newID);
        pss.setIri("graph", modelID+"#PositionGraph");
        pss.setCommandText(query);

        logger.info(pss.toString());
        logger.log(Level.WARNING, "Updating references in "+modelID.toString()+"#PositionGraph");

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    /**
     * Renames Predicate IRI:s
     * @param modelID Model IRI
     * @param oldID Old Predicate IRI
     * @param newID New Predicate IRI
     */
    public static void updateResourceReferencesInModel(IRI modelID, IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?graph { ?any ?predicate ?oldID }} "
                + " INSERT { GRAPH ?graph { ?any ?predicate ?newID }} "
                + " WHERE { "
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph . } "
                + "GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . ?any ?predicate ?oldID . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID", oldID);
        pss.setIri("newID", newID);
        pss.setIri("model", modelID);
        pss.setIri("hasPartGraph", modelID+"#HasPartGraph");
        pss.setCommandText(query);

        logger.info(pss.toString());
        logger.log(Level.WARNING, "Updating references in "+modelID.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }
    
    /**
     * Renames Predicate IRI:s
     * @param modelID Model IRI
     * @param oldID Old Predicate IRI
     * @param newID New Predicate IRI
     */
    public static void updatePredicateReferencesInModel(IRI modelID, IRI oldID, IRI newID) {

        String query
                = " DELETE { GRAPH ?graph { ?any sh:predicate ?oldID }} "
                + " INSERT { GRAPH ?graph { ?any sh:predicate ?newID }} "
                + " WHERE { "
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph . } "
                + "GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . ?any sh:predicate ?oldID}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID", oldID);
        pss.setIri("newID", newID);
        pss.setIri("model", modelID);
        pss.setIri("hasPartGraph", modelID+"#HasPartGraph");
        pss.setCommandText(query);

        logger.info(pss.toString());
        logger.log(Level.WARNING, "Updating references in "+modelID.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    /**
     * Inserts Resource reference to models HasPartGraph and model reference to Resource graph
     * @param graph Resource graph IRI as String
     * @param model Model graph IRI as String
     */
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
    
    /**
     * Insert new graph reference to export graph. In some cases it makes more sense to do small changes than create the whole export graph again.
     * @param graph Resource IRI as String
     * @param model Model IRI as String
     */
    public static void insertNewGraphReferenceToExportGraph(String graph, String model) {

        String query
                = " INSERT { "
                + "GRAPH ?exportGraph { "
                + "?model dcterms:hasPart ?graph . "
                + "}} WHERE { "
                + "GRAPH ?exportGraph { "
                + "?model a owl:Ontology . "
                + "}}";
                

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph", graph);
        pss.setIri("model", model);
        pss.setIri("exportGraph", model+"#ExportGraph");
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();

    }
    
    /**
     * Add existing Resource to the model as Resource reference. Inserts only the reference to models HasPartGraph.
     * @param graph Resource IRI as String
     * @param model Model IRI as String
     */
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

    /**
     * Removes Resource-graph reference from models HasPartGraph
     * @param graph Resource IRI reference to be removed
     * @param model Model IRI
     */
    public static void deleteGraphReferenceFromModel(IRI graph, IRI model) {

        String query
                = " DELETE { "
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph } "
                + "} "
                + " WHERE { "
                + "GRAPH ?model { ?model a ?type . } "
                + "GRAPH ?hasPartGraph { ?model dcterms:hasPart ?graph } "
                + "}";

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

    /**
     * Copies graph from one Service to another Service
     * @param fromGraph Existing graph in original service as String
     * @param toGraph  New graph IRI as String
     * @param fromService Service where graph exists
     * @param toService Service where graph is copied
     * @throws NullPointerException
     */
    public static void addGraphFromServiceToService(String fromGraph, String toGraph, String fromService, String toService) throws NullPointerException {
        
        DatasetAccessor fromAccessor = DatasetAccessorFactory.createHTTP(fromService);
        Model graphModel = fromAccessor.getModel(fromGraph);
        
        if(graphModel==null) {
            throw new NullPointerException();
        }
        
        DatasetAccessor toAccessor = DatasetAccessorFactory.createHTTP(toService);
        toAccessor.add(toGraph, graphModel);
        
    }
        
    /**
     * Copies graph to another graph in Core service
     * @param fromGraph Graph IRI as string
     * @param toGraph New copied graph IRI as string
     * @throws NullPointerException
     */
    public static void addCoreGraphToCoreGraph(String fromGraph, String toGraph) throws NullPointerException {
        
        DatasetAccessor fromAccessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        Model graphModel = fromAccessor.getModel(fromGraph);
        
        if(graphModel==null) {
            throw new NullPointerException();
        }
        
        fromAccessor.putModel(toGraph, graphModel);
    }

    /**
     * Put model to graph
     * @param model Jena model
     * @param id IRI of the graph as String
     */
    public static void putToGraph(Model model, String id) {
        
      DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
      DatasetAdapter adapter = new DatasetAdapter(accessor);
      
      adapter.putModel(id, model);
 
        
    }
    
    /**
     * Constructs JSON-LD from graph
     * @param query SPARQL query as string
     * @return Returns JSON-LD object
     */
    public static String constructFromGraph(String query){
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);
        Model results = qexec.execConstruct();
        return ModelManager.writeModelToString(results);
       
    }

    /**
     * Returns date when the model was last modified from the Export graph
     * @param graphName Graph IRI as string
     * @return Returns date
     */
    public static Date lastModified(String graphName) {
    
     ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources =
                "SELECT ?date WHERE { "
                + "GRAPH ?exportGraph { "+
                " ?graph a owl:Ontology . "
                + "?graph dcterms:modified ?date . "+
                "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        pss.setIri("graph",graphName);
        pss.setIri("exportGraph",graphName+"#ExportGraph");

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        Date modified = null;
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if(soln.contains("date")) {
                Literal liteDate = soln.getLiteral("date");
                modified = ((XSDDateTime)XSDDatatype.XSDdateTime.parse(liteDate.getString())).asCalendar().getTime();
                }
            }
        
        return modified;
        
    }


    
}
