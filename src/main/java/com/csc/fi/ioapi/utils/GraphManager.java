/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
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
    
    public static void dropAll() {
        
    }
    
    
    
        public static void removeDuplicatesFromModel(String graph) {
        
        String query = 
                " DELETE { GRAPH ?graph { ?graph owl:imports ?import . ?resource ?p ?o . ?resource sh:property ?propertyID . ?propertyID ?pp ?oo . }}"+
                " WHERE { GRAPH ?graph { ?graph owl:imports ?import .  OPTIONAL { VALUES ?type { sh:ShapeClass owl:DatatypeProperty owl:ObjectProperty } ?resource a ?type . ?resource ?p ?o . OPTIONAL { ?resource sh:property ?property . ?property ?pp ?oo . } } }}";
                 
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
     
        /* ADD all used in the query to be sure */ 
        pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        
        pss.setIri("graph",graph);
        pss.setCommandText(query);        
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
    }
    
    public static void createResourceGraphs(String graph, Map<String,String> map) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
        deleteResourceGraphs(graph);
        
        // GRAPH ?resource { ?resource ?p ?o .  ?resource sh:property ?node . ?node ?pp ?oo .  ?resource a ?type . ?resource dcterms:modified ?date . ?resource rdfs:isDefinedBy ?graph . } 
        // . ?resource a ?type . ?resource ?p ?o . OPTIONAL { ?resource sh:property ?node . ?node ?pp ?oo . } }    
        
        /* Creates resource graphs from model and adds UUIDS for class propeties */
        String query = 
                " INSERT { "
                + "GRAPH ?graph { "
                + " ?graph dcterms:hasPart ?resource . "
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
                + " VALUES ?type { sh:ShapeClass owl:DatatypeProperty owl:ObjectProperty } "
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
        
        pss.setIri("graph",graph);
        pss.setLiteral("date", timestamp);
        pss.setLiteral("draft", "Unstable");
        pss.setCommandText(query);        
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
        
       updateResourceGraphs(graph, map);
        
    }
    
    public static void updateResourceGraphs(String model, Map<String,String> map) {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?resource WHERE { GRAPH ?resource { ?resource rdfs:isDefinedBy ?model . }}";    

        pss.setIri("model", model);
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect() ;

        while (results.hasNext())
        {
          QuerySolution soln = results.nextSolution() ;
          constructGraphs(model,soln.getResource("resource").toString(),map);
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

        ResultSet results = qexec.execSelect() ;
        int id = 1;
        while (results.hasNext())
        {
          QuerySolution soln = results.nextSolution() ;
          addIndexToProperty(resource,soln.getResource("property").toString(),id++);
        }

    }

    public static void addIndexToProperty(String resource, String property, int index) {
            
        String query = 
                " INSERT { "
                + "GRAPH ?resource { "
                + "?resource sh:property ?property . "
                + "?property sh:index ?index . }}"
                + "WHERE { "
                + " GRAPH ?resource { "
                + "?resource sh:property ?property . "
                + "}}";
                 
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

        pss.setIri("resource",resource);
        pss.setIri("property",property);
        pss.setLiteral("index", index);
        pss.setCommandText(query);        
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
    
    public static void constructGraphs(String graph, String resource, Map<String,String> map) {
        
    ParameterizedSparqlString pss = new ParameterizedSparqlString(); 
    
    String query = 
              "CONSTRUCT { "
            + "?resource a ?type . "
            + "?resource owl:versionInfo ?draft . "
            + "?resource ?p ?o .  "
            + "?resource sh:property ?uuid . "
            + "?uuid owl:versionInfo ?draft . "
            + "?uuid ?pp ?oo .  "
            + "?resource dcterms:modified ?date . "
            + "?resource rdfs:isDefinedBy ?graph . } "
            + " WHERE { "
            + "GRAPH ?graph { "
            + "VALUES ?type { sh:ShapeClass owl:DatatypeProperty owl:ObjectProperty } . "
            + "?resource a ?type . "
            + "?resource ?p ?o . "
            + "FILTER(!isBlank(?o)) "
            + "OPTIONAL { "
            + " ?resource sh:property ?property . "
            + " ?property sh:predicate ?predicate . "
            + " ?property ?pp ?oo . } }  "
            + "GRAPH ?resource { "
            + " OPTIONAL { "
            + "  ?resource sh:property ?uuid . "
            + "  ?uuid sh:predicate ?predicate ."
            + "}} "
            + "}";
    
    pss.setIri("graph", graph);
    pss.setIri("resource", resource);
    pss.setLiteral("draft","Unstable");
    pss.setNsPrefixes(map);
    pss.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
    pss.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
    pss.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
    pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        
    pss.setCommandText(query);
    
    QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());
    
    Model results = qexec.execConstruct();
   // results.write(System.out, "TURTLE");
    
    DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
    accessor.add(resource, results);
  
    }
    

    public static void deleteResourceGraphs(String model) {
        
        String query = "DELETE WHERE { GRAPH ?graph { ?s ?p ?o . ?graph rdfs:isDefinedBy ?model . } }";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pss.setIri("model", model);
        
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
        /* OPTIONALLY
        
       UpdateRequest request = UpdateFactory.create() ;
        request.add("DROP ALL")
        UpdateAction.execute(request, graphStore) ;
        
        */
    }
    
    
    public static boolean testDefaultGraph() {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { ?s a sd:Service ; sd:defaultDataset ?d . ?d sd:defaultGraph ?g . ?g dcterms:title ?title . }";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query,"urn:csc:iow:sd");
        
         try
          {
              boolean b = qexec.execAsk();
              return b;
           } catch(Exception ex) {
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
        
         try
          {
              boolean b = qexec.execAsk();
              return b;
           } catch(Exception ex) {
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
        
         try
          {
              boolean b = qexec.execAsk();
              return b;
           } catch(Exception ex) {
               return false; 
           }
    }
    
      public static void createDefaultGraph() {
        
 
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGraphInputStream(),RDFLanguages.JSONLD);
      
        accessor.putModel("urn:csc:iow:sd",m);
        
    }

    public static String buildRemoveModelQuery(String model) {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?graph WHERE { GRAPH ?model { ?model dcterms:hasPart ?graph . } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . }}";    

        pss.setIri("model", model);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect() ;
        String newQuery = "DROP SILENT GRAPH <"+model+">; ";

        while (results.hasNext())
        {
          QuerySolution soln = results.nextSolution() ;
          newQuery += "DROP SILENT GRAPH <"+soln.getResource("graph").toString()+">; ";
        }
     
        return newQuery;
     
    }
    
    public static Map<String,String> getNamespaceMap() {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = 
                "SELECT ?namespace ?prefix WHERE { "
                + "GRAPH ?graph { "
                + " ?graph a owl:Ontology .  "
                + " ?graph dcap:preferredXMLNamespaceName ?namespace . "
                + " ?graph dcap:preferredXMLNamespacePrefix ?prefix . "
                + "}}";    

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect() ;
        Map namespaceMap = new HashMap<String,String>();
        
        while (results.hasNext())
        {
          QuerySolution soln = results.nextSolution() ;
          namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
        }
     
      return namespaceMap;
        
    }
    

    public static void removeModel(IRI id) {
       
        String query = buildRemoveModelQuery(id.toString());
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.info("Removing model from "+id);
        logger.info(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
     
        try {
           qexec.execute();
        } catch(UpdateException ex) {
           logger.log(Level.WARNING, ex.toString());
        }
    }   
      
       public static void removeGraph(IRI id) {
       
        String query = "DROP GRAPH ?graph ;";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        pss.setIri("graph", id);

        logger.log(Level.WARNING, "Removing graph "+id);
            
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
     
        try {
           qexec.execute();
        } catch(UpdateException ex) {
           logger.log(Level.WARNING, ex.toString());
        }
    }   
      
    public static void deleteGraphs() {
       
        String query = "DROP ALL";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        
        logger.log(Level.WARNING, pss.toString()+" from "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
       
    }
    
    
        public static void renameID(IRI oldID, IRI newID) {
     
       String query =
                " DELETE { GRAPH ?graph { ?graph dcterms:hasPart ?oldID }}"+
                " INSERT { GRAPH ?graph { ?graph dcterms:hasPart ?newID }}"+
                " WHERE { GRAPH ?graph { ?graph dcterms:hasPart ?oldID }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("oldID",oldID);
        pss.setIri("newID",newID);
        pss.setCommandText(query);
        
        logger.log(Level.WARNING, "Renaming "+oldID+" to "+newID);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    public static void insertNewGraphReferenceToModel(IRI graph, IRI model) {
     
       String timestamp = SafeDateFormat.fmt().format(new Date());
        
       String query = 
                " INSERT { GRAPH ?model { ?model dcterms:hasPart ?graph } GRAPH ?graph { ?graph rdfs:isDefinedBy ?model . ?graph dcterms:created ?timestamp . }} "+
                " WHERE { GRAPH ?graph {}}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setIri("model",model);
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);
        
        logger.log(Level.WARNING, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    public static void insertExistingGraphReferenceToModel(IRI graph, IRI model) {
     
      // TODO: ADD MODIFIED DATE TO MODEL
     //   String timestamp = fmt.format(new Date());
        
         String query = 
                " INSERT { GRAPH ?model { ?model dcterms:hasPart ?graph }} "+
                " WHERE { GRAPH ?graph { ?graph a ?type . }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setIri("model",model);
       // pss.setLiteral("date", timestamp);
        pss.setCommandText(query);
        
        logger.log(Level.WARNING, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
    public static void deleteGraphReferenceFromModel(IRI graph, IRI model) {

         String query = 
                " DELETE { GRAPH ?model { ?model dcterms:hasPart ?graph }} "+
                " WHERE { GRAPH ?graph { ?graph a ?type . }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setIri("model",model);
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
    public static void updateModelNamespaceInfo(String model, String namespace, String prefix) {
        
       String query = 
                " INSERT { GRAPH ?model { ?model dcap:preferredXMLNamespaceName ?namespace . ?model dcap:preferredXMLNamespacePrefix ?prefix . }} "+
                " WHERE { GRAPH ?model { ?model ?p ?o . }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model",model);
        pss.setLiteral("namespace",namespace);
        pss.setLiteral("prefix", prefix);
        pss.setCommandText(query);
        
        logger.info(pss.toString());
  
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
}
