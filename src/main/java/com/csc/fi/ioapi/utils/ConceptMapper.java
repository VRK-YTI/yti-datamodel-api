/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import static com.csc.fi.ioapi.utils.GraphManager.services;
import static com.csc.fi.ioapi.utils.ImportManager.services;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import static org.apache.jena.vocabulary.RDFS.Nodes.Resource;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

/**
 *
 * @author malonen
 */
public class ConceptMapper {
    
     private static EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ConceptMapper.class.getName());
    
      public static void updateConceptFromConceptService(String uri) {
        
        /* Only if concept IDs are not local UUIDs */ 
        if(!uri.startsWith("urn:uuid:")) {   
            
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getTempConceptReadWriteAddress());
            
            if(!accessor.containsModel(uri)) {
         
                Client client = Client.create();

                WebResource webResource = client.resource(services.getConceptAPI())
                                          .queryParam("uri", UriComponent.encode(uri,UriComponent.Type.QUERY))
                                          .queryParam("format","application/json");

                WebResource.Builder builder = webResource.accept("application/json");
                ClientResponse response = builder.get(ClientResponse.class);

                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    logger.warning("Could not find the concept");
                }

                Model model = ModelFactory.createDefaultModel(); 

                RDFDataMgr.read(model, response.getEntityInputStream(), RDFLanguages.JSONLD);
                
                accessor.add(uri, model);

                logger.info("Updated "+uri+" from "+services.getConceptAPI());
                
            } else {
                logger.info("Concept already exists!");
            }
        } 

  
    }    
     
    public static void addConceptFromReferencedResource(String model, String classID) {
        
        resolveConcept(classID);
                
        String query
                = "INSERT { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + "WHERE { "
                + "SERVICE ?modelService {"
                + "GRAPH ?class {"
                + "?class dcterms:subject ?concept . "
                + "}"
                + "}"
                + "GRAPH ?concept { ?s ?p ?o . } }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("class", classID);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setCommandText(query);
        
        logger.info("ADDING CONCEPT from "+classID);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();
        
       // removeReferencesFromCollection(model);
        removeUnusedConcepts(model);
          
    }
    
    public static void removeUnusedConcepts(String model) {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT DISTINCT ?subject "
                + "WHERE { "
                + "{GRAPH ?skosCollection { ?skosCollection skos:member ?subject . }} "
                + " MINUS "
                + "{SERVICE ?modelService {"
                + "GRAPH ?hasPartGraph { "
                + " ?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource rdfs:isDefinedBy ?model . "
                + "?resource dcterms:subject ?subject . "
                + "}}}"
                + "}";

        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setIri("model", model);
        pss.setIri("hasPartGraph", model+"#HasPartGraph");
        pss.setIri("skosCollection", model+"/skos#");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        
        logger.info(pss.toString());
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getTempConceptReadSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            Resource resSubject = soln.getResource("subject");
            if(resSubject!=null) {
            String subject = soln.getResource("subject").toString();
            logger.info("Unused concept: "+subject);
            deleteConceptReference(model,subject);
            
            if(!isUsedConceptGlobal(subject)) adapter.deleteModel(subject);
            
            }
        }

    }
    
    public static void removeReferencesFromCollection(String model) {
                
         String query
                = "DELETE { GRAPH ?skosCollection { ?skosCollection skos:member ?subject . } }"
                + " WHERE { "
                + "SERVICE ?modelService {"
                + "GRAPH ?hasPartGraph { "
                + " ?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource rdfs:isDefinedBy ?model . "
                + "?resource dcterms:subject ?usedSubject  "
                + "}}"
                + "GRAPH ?skosCollection { ?skosCollection skos:member ?subject . FILTER(?subject!=?usedSubject) }"
                +"}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", model);
        pss.setIri("hasPartGraph", model+"#HasPartGraph");
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();
    }
    
    public static void resolveConcept(String resource) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?subject WHERE { GRAPH ?resource { ?resource dcterms:subject ?subject . }}";

        pss.setIri("resource", resource);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
             Resource resSubject = soln.getResource("subject");
            if(resSubject!=null) {
                String subject = resSubject.toString();
                ConceptMapper.updateConceptFromConceptService(subject);
            }
        }
    }
     

    public static void addConceptToLocalSKOSCollection(String model, String concept) {
        
        String query
                = " INSERT { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + " WHERE { GRAPH ?concept { ?s ?p ?o . } }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("concept", concept);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();

    }
    
        public static boolean deleteModelReference(String model, String concept) {
            
            if(isUsedConcept(model, concept)) {
               return false;
            }
            else {
             deleteConceptReference(model, concept);  
             return true;
            }
        }
        
        
    public static boolean isUsedConcept(String model, String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s rdfs:isDefinedBy ?model . ?s ?p ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);
        pss.setIri("model",model);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    
        public static boolean isUsedConceptGlobal(String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?graph dcterms:subject ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
    
    
    public static void deleteConceptReference(String model, String concept) {
        
          
        String query
                = " DELETE { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + " WHERE { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("concept", concept);
        pss.setCommandText(query);
        

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();

    }
        
    
    public static void deleteConceptSuggestion(String model, String concept) {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        
        adapter.deleteModel(concept);
                   
       deleteConceptReference(model, concept);

    }

    
}
