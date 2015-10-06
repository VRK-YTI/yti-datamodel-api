/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    
    final static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    static EndpointServices services = new EndpointServices();
    
    public static void dropAll() {
        
    }
    
    public static void createResourceGraphs(String graph) {
        
        String timestamp = fmt.format(new Date());
        
         String query = 
                " INSERT { GRAPH ?graph { ?graph dcterms:hasPart ?resource } GRAPH ?resource { ?resource ?p ?o . ?resource sh:property ?props . ?props ?pp ?po .  ?resource a ?type . ?resource dcterms:modified ?date . ?resource rdfs:isDefinedBy ?graph . } } "+
                " WHERE { GRAPH ?graph { VALUES ?type { sh:ShapeClass owl:DatatypeProperty owl:ObjectProperty } . ?resource a ?type . ?resource ?p ?o . OPTIONAL { ?resource sh:property ?props . ?props ?pp ?po . } }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.INFO, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
      public static void createDefaultGraph() {
        
 
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGraphInputStream(),RDFLanguages.JSONLD);
      
        accessor.putModel("urn:csc:iow:sd",m);
        
    }
    
    
    public static void deleteGraphs() {
       
        String query = 
                "DROP ALL";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
       // pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(query);
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" from "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
       
    }
    
    
    
        public static void deleteResourceGraphs(String graph) {
        
         String query = 
                " DELETE { GRAPH ?resource { ?s ?p ?o } } "+
                " WHERE { GRAPH ?graph {?resource a ?type . } GRAPH ?resource {?s ?p ?o . } }";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setCommandText(query);
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
        
        
    public static void insertNewGraphReferenceToModel(IRI graph, IRI model) {
     
       String timestamp = fmt.format(new Date());
        
       String query = 
                " INSERT { GRAPH ?model { ?model dcterms:hasPart ?graph } GRAPH ?graph { ?graph rdfs:isDefinedBy ?graph . ?graph dcterms:created ?timestamp . }} "+
                " WHERE { GRAPH ?graph {}}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setIri("model",model);
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
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
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+services.getCoreSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
}
