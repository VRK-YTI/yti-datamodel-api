/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import static com.csc.fi.ioapi.utils.UserManager.services;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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
