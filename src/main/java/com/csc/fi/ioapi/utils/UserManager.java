/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import java.util.Date;
import com.csc.fi.ioapi.config.LoginSession;
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
public class UserManager {
    
    static EndpointServices services = new EndpointServices();
    static final private Logger logger = Logger.getLogger(UserManager.class.getName());

    /**
     * Check if user with given email exists
     * @param email email of the user
     * @return boolean
     */
    private static boolean isExistingUser(String email) {
        
         String queryString = " ASK {?id a foaf:Person . ?id foaf:mbox ?email . }";
    
         ParameterizedSparqlString pss = new ParameterizedSparqlString();
         pss.setNsPrefixes(LDHelper.PREFIX_MAP);
         pss.setLiteral("email", email);
         pss.setCommandText(queryString);
         
        
         Query query = pss.asQuery();
         QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getUsersSparqlAddress(), query,"urn:csc:users");
        
         try
          {
              boolean b = qexec.execAsk();
              return b;
              
           } catch(Exception ex) {
               logger.log(Level.WARNING, "Failed in checking the endpoint status: "+services.getUsersSparqlAddress());
               return false; 
           }
        
    }


    /**
     * Check if user exists in the system and create new if not
     * @param loginSession
     */
    public static void checkUser(LoginSession loginSession) {
        
            if(isExistingUser(loginSession.getEmail())) {
                updateUser(loginSession);
            } else {
                logger.log(Level.INFO, "Creating new user: "+loginSession.getEmail());
                createUser(loginSession);
            }
     
    }

    /**
     * Creates new user
     * @param loginSession Login session
     */
    public static void createUser(LoginSession loginSession) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
        
        String groups = "";
        
        HashMap<String,Boolean> allGroups = loginSession.getGroups();
        
        if(allGroups!=null) {
            
            Iterator<String> groupIterator = allGroups.keySet().iterator();

            while(groupIterator.hasNext()) {
                String group = groupIterator.next();
                Node n = NodeFactory.createURI(group);
                groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . "+(allGroups.get(group).booleanValue()?" ?id iow:isAdminOf <"+n.getURI()+"> . ":"");
            }
        
        }
        
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
        

        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
       
 
    }

    /**
     * Delete ALL users
     */
    @Deprecated
    public static void deleteUsers() {
       
        String query = 
                "DROP ALL";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
       // pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(query);
        
        logger.log(Level.WARNING, pss.toString()+" from "+services.getUsersSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
       
    }

    /**
     * Update users groups from session
     * @param loginSession Login session
     */
    public static void updateUser(LoginSession loginSession) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
        
        String groups = "";
        
        HashMap<String,Boolean> allGroups = loginSession.getGroups();
        
        if(allGroups!=null) {
            Iterator<String> groupIterator = allGroups.keySet().iterator();

            while(groupIterator.hasNext()) {
                String group = groupIterator.next();
                Node n = NodeFactory.createURI(group);
                logger.log(Level.INFO, group+" is "+allGroups.get(group).booleanValue());
                groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . "+(allGroups.get(group).booleanValue()?" ?id iow:isAdminOf <"+n.getURI()+"> . ":"");
            }
        }
         
         String query = 
                "WITH <urn:csc:users> "+
                "DELETE { ?id dcterms:modified ?oldTime . ?id dcterms:isPartOf ?partOfgroup . ?id iow:isAdminOf ?adminOfgroup . ?id foaf:name ?name . } "+
                "INSERT { ?id foaf:name ?newName . ?id dcterms:modified ?timestamp . "+ groups +
                "} WHERE {"+
                "?id a foaf:Person . "
               + "OPTIONAL { ?id foaf:name ?name . }"
               + "OPTIONAL {?id dcterms:modified ?oldTime . } "
               + "OPTIONAL {?id dcterms:isPartOf ?partOfgroup .} "
               + "OPTIONAL {?id iow:isAdminOf ?adminOfgroup .}}";
                 
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("id","mailto:"+loginSession.getEmail());
        pss.setLiteral("newName", loginSession.getDisplayName());
        pss.setLiteral("mail", loginSession.getEmail());
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
        

    }
        


}
