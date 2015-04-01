/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.core.Response;
import org.joda.time.*;
import org.joda.time.format.*;

/**
 *
 * @author malonen
 */
public class ServiceDescriptionManager {
    
    final static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    public static void updateGraphDescription(String service, String graph) {
        
        String timestamp = fmt.format(new Date());
        
        System.out.println(timestamp);
        
        String query = LDHelper.prefix +
                "DELETE { "+
                " ?graph dcterms:modified ?date . "+
                "} "+
                "INSERT { "+
                " ?graph dcterms:modified '"+timestamp+"'^^xsd:dateTime . "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:defaultDataset ?dataset . "+
                " ?dataset sd:namedGraph ?graph . "+
                " ?graph sd:name <"+graph+"> . "+
                " OPTIONAL {?graph dcterms:modified ?date . }"+
                "}";
       
        UpdateRequest queryObj=UpdateFactory.create(query);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
      
    }
    
        public static void createGraphDescription(String service, String graph) {

        String timestamp = fmt.format(new Date());
       
        String query = LDHelper.prefix +
                "INSERT { "+
                " ?dataset sd:namedGraph _:graph . "+
                " _:graph a sd:NamedGraph . "+
                " _:graph sd:name <"+graph+"> . "+
                " _:graph dcterms:created '"+timestamp+"'^^xsd:dateTime . "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:defaultDataset ?dataset . "+
                " FILTER NOT EXISTS { "+
                " ?dataset sd:namedGraph ?graph . "+
                " ?graph sd:name <"+graph+"> . "+
                "}}";
        
        System.out.println(query);
       
        UpdateRequest queryObj=UpdateFactory.create(query);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
      
    }
        
        
    public static void deleteGraphDescription(String service, String graph) {
        
        String query = LDHelper.prefix +
                "DELETE { "+
                " ?graph ?p ?o "+
                "} WHERE {"+
                " ?service a sd:Service . "+
                " ?service sd:defaultDataset ?dataset . "+
                " ?dataset sd:namedGraph ?graph . "+
                " ?graph sd:name <"+graph+"> . "+
                " ?graph ?p ?o "+
                "}";
       
        UpdateRequest queryObj=UpdateFactory.create(query);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
      
    }
    
    
}
