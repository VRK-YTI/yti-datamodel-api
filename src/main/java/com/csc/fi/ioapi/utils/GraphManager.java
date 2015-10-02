/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.iri.IRI;

/**
 *
 * @author malonen
 */



public class GraphManager {
    
    final static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    public static String ModelSparqlUpdateEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/update";
    }
    
    public static void createResourceGraphs(String graph) {
        
        String timestamp = fmt.format(new Date());
        
         String query = 
                " INSERT { GRAPH ?graph { ?graph dcterms:hasPart ?resource } GRAPH ?resource { ?resource ?p ?o . ?o ?op ?oo . ?resource a ?type . ?resource dcterms:modified ?date . ?resource rdfs:isDefinedBy ?graph . } } "+
                " WHERE { GRAPH ?graph { VALUES ?type { sh:ShapeClass owl:DatatypeProperty owl:ObjectProperty } . ?resource a ?type . ?resource ?p ?o . OPTIONAL { ?o ?op ?oo } }}";
                    
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graph",graph);
        pss.setLiteral("date", timestamp);
        pss.setCommandText(query);
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+ModelSparqlUpdateEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,ModelSparqlUpdateEndpoint());
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
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+ModelSparqlUpdateEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,ModelSparqlUpdateEndpoint());
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
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+ModelSparqlUpdateEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,ModelSparqlUpdateEndpoint());
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
        
        Logger.getLogger(GraphManager.class.getName()).log(Level.WARNING, pss.toString()+" "+ModelSparqlUpdateEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,ModelSparqlUpdateEndpoint());
        qexec.execute();
        
    }
    
    
}
