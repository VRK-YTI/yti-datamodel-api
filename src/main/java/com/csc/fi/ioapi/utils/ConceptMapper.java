/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import static com.csc.fi.ioapi.utils.GraphManager.services;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

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
            
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getTempConceptReadWriteAddress());
            accessor.add(uri, model);
            
            logger.info("Updated "+uri+" from "+services.getConceptAPI());
            
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
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();

    }

    
}
