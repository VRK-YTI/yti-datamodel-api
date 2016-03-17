/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import static com.csc.fi.ioapi.utils.UserManager.services;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

/**
 *
 * @author malonen
 */
public class GroupManager {
    
    
     public static boolean testDefaultGroups() {
         
         String queryString = "ASK { ?s a foaf:Group . }";
    
         Query query = QueryFactory.create(LDHelper.prefix+queryString);        
         QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);
        
         try
          {
              boolean b = qexec.execAsk();
              
              return b;
              
           } catch(Exception ex) {
              return false; 
           }
    }
    
  
   public static void createDefaultGroups() {
       
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGroupsInputStream(), RDFLanguages.JSONLD);
      
        accessor.putModel("urn:csc:groups", m);
        
    }
    

    
}
