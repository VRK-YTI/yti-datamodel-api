/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import java.util.Date;
import com.csc.fi.ioapi.config.LoginSession;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

/**
 *
 * @author malonen
 */
public class UserManager {
    
    static EndpointServices services = new EndpointServices();

    
    

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
               Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, "Failed in checking the endpoint status: "+services.getUsersSparqlAddress());
               return false; 
           }
        
    }
    

    
    public static void checkUser(LoginSession loginSession) {
        
        if(isExistingUser(loginSession.getEmail())) {
            updateUser(loginSession);
        } else {
            Logger.getLogger(UserManager.class.getName()).log(Level.INFO, "Creating new user: "+loginSession.getEmail());
            createUser(loginSession);
        }
        
    }
    
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
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" "+services.getUsersSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
       
 
    }
    
    public static void deleteUsersAndGroups() {
       
        String query = 
                "DROP ALL";
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
       // pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(query);
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" from "+services.getUsersSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
       
    }
    
    
        public static void updateUser(LoginSession loginSession) {
        
        String timestamp = SafeDateFormat.fmt().format(new Date());
        
        
        String groups = "";
        
        
        HashMap<String,Boolean> allGroups = loginSession.getGroups();
        
        if(allGroups!=null) {
            Iterator<String> groupIterator = allGroups.keySet().iterator();

            while(groupIterator.hasNext()) {
                String group = groupIterator.next();
                Node n = NodeFactory.createURI(group);
                Logger.getLogger(UserManager.class.getName()).log(Level.INFO, group+" is "+allGroups.get(group).booleanValue());
                groups = groups+"?id dcterms:isPartOf <"+n.getURI()+"> . "+(allGroups.get(group).booleanValue()?" ?id iow:isAdminOf <"+n.getURI()+"> . ":"");
            }
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
                "?id a foaf:Person . OPTIONAL {?id dcterms:modified ?oldTime .} OPTIONAL {?id dcterms:isPartOf ?group .} OPTIONAL {?id iow:isAdminOf ?group .}}";
                 
         
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("id","mailto:"+loginSession.getEmail());
        pss.setLiteral("name", loginSession.getDisplayName());
        pss.setLiteral("mail", loginSession.getEmail());
        pss.setLiteral("timestamp", timestamp,XSDDatatype.XSDdateTime);
        pss.setCommandText(query);
        
        Logger.getLogger(UserManager.class.getName()).log(Level.WARNING, pss.toString()+" "+services.getUsersSparqlUpdateAddress());
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj,services.getUsersSparqlUpdateAddress());
        qexec.execute();
        

    }
        
        public static boolean testDefaultGroups() {
         
         String queryString = "ASK { ?s a foaf:Group . }";
    
         Query query = QueryFactory.create(LDHelper.prefix+queryString);        
         QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getUsersSparqlAddress(), query);
        
         try
          {
              boolean b = qexec.execAsk();
              
              return b;
              
           } catch(Exception ex) {
              return false; 
           }
    }
    
 
    public static void createDefaultGroups() {
       
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getUsersReadWriteAddress());
        
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGroupsInputStream(),RDFLanguages.JSONLD);
      
        accessor.putModel(m);
        
    }
        

}
