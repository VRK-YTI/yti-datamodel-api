/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import static com.csc.fi.ioapi.utils.GraphManager.services;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

/**
 *
 * @author malonen
 */
public class NamespaceManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(NamespaceManager.class.getName());
      
    public static boolean isSchemaInStore(String namespace) {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        return adapter.containsModel(namespace);
        
    }

    public static void putSchemaToStore(String namespace, Model model) {
        
      	DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadWriteAddress());
	DatasetAdapter adapter = new DatasetAdapter(accessor);
	adapter.add(namespace, model);
    
    }
    
    /* Get exact namespaces from graph */
    public static Map<String, String> getCoreNamespaceMap(String graph, String service) {
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(service);
        Model classModel = accessor.getModel(graph);
            
            if(classModel==null) {
                return null;
            }
            
            return classModel.getNsPrefixMap();
       
    }
    
    public static void copyNamespacesFromGraphToGraph(String fromGraph, String toGraph, String fromService, String toService) throws NullPointerException {
        
        /* TODO: j.0 namespace ISSUE!? */ 
        
        DatasetAccessor fromAccessor = DatasetAccessorFactory.createHTTP(fromService);
        Model classModel = fromAccessor.getModel(fromGraph);
            
        if(classModel==null) {
            throw new NullPointerException();
        }
            
        Map<String, String> namespaces = classModel.getNsPrefixMap();
        
        Model copyNamespaces = ModelFactory.createDefaultModel();
        copyNamespaces.setNsPrefixes(namespaces);
        copyNamespaces.removeNsPrefix("j.0");
        
        DatasetAccessor toAccessor = DatasetAccessorFactory.createHTTP(toService);
        toAccessor.add(toGraph, copyNamespaces);
        
    }
    
    /* Get namespaces from all graphs */
    public static Map<String, String> getCoreNamespaceMap() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources
                = "SELECT ?namespace ?prefix WHERE { "
                + "GRAPH ?graph { "
                + " ?graph a ?type  "
                + " VALUES ?type { owl:Ontology dcap:DCAP }"
                + " ?graph dcap:preferredXMLNamespaceName ?namespace . "
                + " ?graph dcap:preferredXMLNamespacePrefix ?prefix . "
                + "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        Map namespaceMap = new HashMap<String, String>();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
        }

        return namespaceMap;

    }



}
