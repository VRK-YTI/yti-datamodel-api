/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.Endpoint;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.csc.fi.ioapi.config.LoginSession;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 *
 * @author malonen
 */
public class UserManager {
    
    final static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    public static String userEndpoint() {
       return Endpoint.getEndpoint() + "/users/data";
    }

    public static void createUser(LoginSession loginSession) {
        
        String timestamp = fmt.format(new Date());
        
        
        String groups = "";
        for(String g : loginSession.getGroupUris()) {
            Node n = NodeFactory.createURI(g);
            groups.concat("?id dcterms:isPartOf <".concat(n.getURI()).concat("> "));
        }
        
         String query = 
                "WITH <urn:csc:users>"+
                "INSERT { ?id a foaf:Person . "+
                " ?id foaf:name ?name . "+
                 "?id dcterms:created ?timestamp . "+
                " ?id foaf:mbox ?mail . "+ groups +
                "} WHERE {}";
        
          System.out.println(query);
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setParams(null);
        pss.setIri("name", loginSession.getDisplayName());
        pss.setIri("mail", loginSession.getEmail());
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,userEndpoint());
        qexec.execute();
        
 
    }
    /*
        public static void updateGraphDescription(String service, String graph) {
        
        String timestamp = fmt.format(new Date());
        
        System.out.println(timestamp);
        
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
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
      
    }
        
    public static void deleteGraphDescription(String service, String graph) {
        
        String query =
                "WITH <urn:csc:iow:sd>"+
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

        pss.setLiteral("graphName", graph);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
        
      
    }
    
 */
    
}
