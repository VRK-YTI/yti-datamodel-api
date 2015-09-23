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
public class UserManager {
    
    final static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    public static String userEndpoint() {
       return Endpoint.getEndpoint() + "/users/update";
    }

    private static boolean isExistingUser(String email) {
        
         String queryString = " ASK {?id a foaf:Person . ?id foaf:mbox ?email . }";
    
         ParameterizedSparqlString pss = new ParameterizedSparqlString();
         pss.setNsPrefixes(LDHelper.PREFIX_MAP);
         pss.setLiteral("email", email);
         pss.setCommandText(queryString);
         
         String endpoint = Endpoint.getEndpoint()+"/users/sparql";
        
         Query query = pss.asQuery();
         QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query,"urn:csc:users");
        
         try
          {
              boolean b = qexec.execAsk();
              
              return b;
              
           } catch(Exception ex) {
               Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, "Failed in checking the endpoint status: "+endpoint);
               return false; 
           }
        
    }
    
    public static void checkUser(LoginSession loginSession) {
        
        if(isExistingUser(loginSession.getEmail())) {
            updateUser(loginSession);
        } else {
            createUser(loginSession);
        }
        
    }
    
    public static void createUser(LoginSession loginSession) {
        
        String timestamp = fmt.format(new Date());
        
        
        String groups = "";
        
        HashMap allGroups = loginSession.getGroups();
        Iterator<String> groupIterator = allGroups.keySet().iterator();
        
        while(groupIterator.hasNext()) {
            String group = groupIterator.next();
            Node n = NodeFactory.createURI(group);
            groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . "+(allGroups.get(group).equals(true)?" ?id iow:isAdminOf <"+n.getURI()+"> . ":"");
        }
        
        /*
        for(String g : loginSession.getGroups()) {
            Node n = NodeFactory.createURI(g);
            groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . ";
        }
                */
        
         String query = 
                "INSERT DATA { GRAPH <urn:csc:users> { ?id a foaf:Person . "+
                " ?id foaf:name ?name . "+
                 "?id dcterms:created ?timestamp . "+
                " ?id foaf:mbox ?mail . "+ groups +
                "}}";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("id","mailto:"+loginSession.getEmail());
        pss.setLiteral("name", loginSession.getDisplayName());
        pss.setLiteral("mail", loginSession.getEmail());
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" "+userEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,userEndpoint());
        qexec.execute();
       
 
    }
    
        public static void updateUser(LoginSession loginSession) {
        
        String timestamp = fmt.format(new Date());
        
        
        String groups = "";
        
        HashMap allGroups = loginSession.getGroups();
        Iterator<String> groupIterator = allGroups.keySet().iterator();
        
        while(groupIterator.hasNext()) {
            String group = groupIterator.next();
            Node n = NodeFactory.createURI(group);
            groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . "+(allGroups.get(group).equals(true)?" ?id iow:isAdminOf <"+n.getURI()+"> . ":"");
        }
        
        /*
        for(String g : loginSession.getGroupUris()) {
            Node n = NodeFactory.createURI(g);
            groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . ";
        }*/
        
         String query = 
                "WITH <urn:csc:users> "+
                "DELETE { ?id dcterms:modified ?oldTime . ?id dcterms:isPartOf ?group . ?id iow:isAdminOf ?group . } "+
                "INSERT { ?id dcterms:modified ?timestamp . "+ groups +
                "} WHERE {"+
                "?id a foaf:Person . ?id dcterms:modified ?oldTime . OPTIONAL {?id dcterms:isPartOf ?group .} OPTIONAL {?id iow:isAdminOf ?group .}}";
                 
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("id","mailto:"+loginSession.getEmail());
        pss.setLiteral("name", loginSession.getDisplayName());
        pss.setLiteral("mail", loginSession.getEmail());
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" "+userEndpoint());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,userEndpoint());
        qexec.execute();
        

    }

}
