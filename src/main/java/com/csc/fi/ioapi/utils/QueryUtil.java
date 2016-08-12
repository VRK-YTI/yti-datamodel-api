/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import static com.csc.fi.ioapi.utils.ImportManager.services;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 *
 * @author malonen
 */
public class QueryUtil {
    
    
    public static void main(String args[]) {

        ConceptMapper.updateSchemesFromFinto();
        
        /*
        String query = "PREFIX ex: <http://example.org/>"
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
        + "CONSTRUCT { ex:Example rdfs:label 'Example' . } WHERE { }";
 
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);
        Model results = qexec.execConstruct();
        results.write(System.out, "TTL") ;
        
        Model newModel = ModelFactory.createDefaultModel();
        */
        
        
    }
    
    
}
