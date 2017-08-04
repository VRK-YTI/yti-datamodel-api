/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import java.util.Date;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author malonen
 */
public class ServiceDescriptionManager {
    
    static EndpointServices services = new EndpointServices();
    static final private Logger logger = Logger.getLogger(ServiceDescriptionManager.class.getName());

    /**
     * Updates modified time to service description
     * @param graph graph of the model
     */
    public static void updateGraphDescription(String graph) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
        String query =
                "WITH <urn:csc:iow:sd>"+
                "DELETE { "+
                " ?graph dcterms:modified ?date . "+
                "} "+
                "INSERT { "+
                " ?graph dcterms:modified ?timestamp "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:graphCollection ?graphCollection . "+
                " ?graphCollection sd:namedGraph ?graph . "+
                " ?graph sd:name ?graphName . "+
                " OPTIONAL {?graph dcterms:modified ?date . }"+
                "}";
       
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
      
    }

    /**
     * Checks if model is in the list of groups given
     * @param model ID of the model
     * @param groupList List of group IDs
     * @return boolean
     */
    public static boolean isModelInGroup(String model, HashMap<String,Boolean> groupList) {
        
        Iterator<String> groupIterator = groupList.keySet().iterator();
        
        String groups = "";
        
        if(groupList!=null && !groupList.isEmpty()) {
            groups = "VALUES ?groups { ";

            while(groupIterator.hasNext()) {
                String group = groupIterator.next();
                Node n = NodeFactory.createURI(group);
                groups = groups+" <"+n.getURI()+"> ";
            }

            groups+=" }";
        }
            
         String queryString = " ASK { GRAPH <urn:csc:iow:sd> { "+groups+" ?graph sd:name ?graphName . ?graph dcterms:isPartOf ?groups . }}";
    
         ParameterizedSparqlString pss = new ParameterizedSparqlString();
         pss.setNsPrefixes(LDHelper.PREFIX_MAP);
         pss.setIri("graphName", model);
         pss.setCommandText(queryString);
         
         String endpoint = services.getCoreSparqlAddress();
        
         
         Query query = pss.asQuery();
         QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
        
         try
          {
              boolean b = qexec.execAsk();
              
              return b;
              
           } catch(Exception ex) {
               logger.log(Level.WARNING, "Failed in checking the endpoint status: "+endpoint);
               return false; 
           }
        
    }

    /**
     * Creates graph description for the new model
     * @param graph ID of the graph
     * @param group ID of the group
     * @param userMail User email
     */
    public static void createGraphDescription(String graph, String group, String userMail) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
         String query = 
                "WITH <urn:csc:iow:sd>"+
                "INSERT { ?graphCollection sd:namedGraph _:graph . "+
                " _:graph a sd:NamedGraph . "+
                " _:graph sd:name ?graphName . "+
                " _:graph dcterms:created ?timestamp . "+
                 " _:graph dcterms:isPartOf ?group . "+
                 " _:graph dcterms:creator ?creator . "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:availableGraphs ?graphCollection . "+
                " ?graphCollection a sd:GraphCollection . "+
                " FILTER NOT EXISTS { "+
                " ?graphCollection sd:namedGraph ?graph . "+
                " ?graph sd:name ?graphName . "+
                "}}";
        
          
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        pss.setIri("group", group);
        if(userMail!=null) pss.setIri("creator", "mailto:"+userMail);
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);

        logger.log(Level.FINE, pss.toString());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
 
    }

    /**
     * Deletes graph description
     * @param graph ID of the graph
     */
    public static void deleteGraphDescription(String graph) {
        
        String query =
                "WITH <urn:csc:iow:sd> "+
                "DELETE { "+
                " ?graph ?p ?o "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:availableGraphs ?graphCollection . "+
                " ?graphCollection sd:namedGraph ?graph . "+
                " ?graph sd:name ?graphName . "+
                " ?graph ?p ?o "+
                "}";
       
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        pss.setCommandText(query);

         Logger.getLogger(ServiceDescriptionManager.class.getName()).log(Level.FINE,"Removing "+graph);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
      
    }

    /**
     * Renames ID of the graph in service description
     * @param oldGraph Old graph ID
     * @param newGraph New graph ID
     */
    public static void renameServiceGraphName(String oldGraph, String newGraph) {
    
        String query = 
        "DELETE { "+
         "GRAPH <urn:csc:iow:sd> { "+
  	 "?namedGraph sd:name ?graph . "+
         "}"+
        "}"+
        "INSERT {"+
          "GRAPH <urn:csc:iow:sd> {"+
 	  "?namedGraph sd:name ?newIRI . "+
          "}"+
        "}"+
        "WHERE { "+
          "GRAPH <urn:csc:iow:sd> {"+
          "?graphs sd:namedGraph ?namedGraph ."+
          "?namedGraph sd:name ?graph . }"+
          //"FILTER(?graph=<http://iow.csc.fi/ap/oiliu>)"+
          //"BIND(IRI(STR('http://iow.csc.fi/ns/oiliu')) as ?newIRI) }"
        "}";
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graph", oldGraph);
        pss.setIri("newIRI", newGraph);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
}
