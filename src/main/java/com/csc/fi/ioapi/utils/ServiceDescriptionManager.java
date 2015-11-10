/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import java.util.Date;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
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
            
         String queryString = " ASK { GRAPH <urn:csc:iow:sd> { "+groups+" ?graph sd:name ?graphName . ?graph dcterms:isPartOf ?groups . }";
    
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
               Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, "Failed in checking the endpoint status: "+endpoint);
               return false; 
           }
        
    }
    
    
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

        Logger.getLogger(ServiceDescriptionManager.class.getName()).log(Level.FINE, pss.toString());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
 
    }
        
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
    
    
}
