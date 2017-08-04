/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import static com.csc.fi.ioapi.utils.UserManager.services;
import java.util.logging.Logger;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author malonen
 */
public class GroupManager {
    
    private static final Logger logger = Logger.getLogger(GroupManager.class.getName());

    /**
     * Returns true if groups have changed from the previous version
     * @return Returns boolean
     */
     public static boolean compareDefaultGroups() {
         
         DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
         Model oldM = accessor.getModel("urn:csc:groups");
         
         if(oldM!=null && !oldM.isEmpty()) {
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, LDHelper.getDefaultGroupsInputStream(), RDFLanguages.JSONLD);

            return m.isIsomorphicWith(oldM);
         } else return false;
         
     }

    /**
     *@deprecated
     * Comparing groups instead of number of groups for now
     *
     */
    @Deprecated
    public static boolean testDefaultGroups() {
         
         Model m = ModelFactory.createDefaultModel();
         RDFDataMgr.read(m, LDHelper.getDefaultGroupsInputStream(), RDFLanguages.JSONLD);
         
         ResIterator nodes = m.listResourcesWithProperty(RDF.type);
         int groupCount = nodes.toList().size();
         logger.info("Comparing groups to "+groupCount);
          
         String queryString = "ASK {  "
                 + "{ SELECT (count(distinct ?s) as ?c) WHERE { ?s a foaf:Group . } }"
                 + " FILTER(?c=?groupCount) }";
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(queryString);
        pss.setLiteral("groupCount", groupCount);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
         Query query = QueryFactory.create(pss.toString());        
         QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);
        
         try
          {
              boolean b = qexec.execAsk();
              
              return b;
              
           } catch(Exception ex) {
              return false; 
           }
    }

    /**
     * Creates default groups by reading the JSON file from resources
     */
   public static void createDefaultGroups() {
       
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
        
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, LDHelper.getDefaultGroupsInputStream(), RDFLanguages.JSONLD);
      
        accessor.putModel("urn:csc:groups", m);
        
    }
    

    
}
