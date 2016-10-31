/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.sun.jersey.api.client.ClientResponse;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;

/**
 *
 * @author malonen
 */
public class ResourceManager {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());
   
    public static UUID putNewResource(String id, String model, String body, LoginSession login) {
        
            /* Create new graph */ 
           boolean success = JerseyFusekiClient.graphIsUpdatedToTheService(id, body, services.getCoreReadWriteAddress());

           if (!success) {
               logger.log(Level.WARNING, "Unexpected: Not created: "+id);
               return null;
           }

           UUID provUUID = UUID.randomUUID();
            
           /* If new class was created succesfully create prov activity */
           if(ProvenanceManager.getProvMode()) {
               ProvenanceManager.createProvenanceActivity(id, login.getEmail(), body, provUUID);
           }
            
            GraphManager.insertNewGraphReferenceToModel(id, model);
            
            logger.log(Level.INFO, id+" updated sucessfully!");
            
            ConceptMapper.addConceptFromReferencedResource(model,id);
            
            GraphManager.insertNewGraphReferenceToExportGraph(id,model);
            JerseyFusekiClient.postGraphToTheService(model+"#ExportGraph", body, services.getCoreReadWriteAddress());
           
            return provUUID;
    }
    
    public static UUID updateResourceWithNewId(IRI idIRI, IRI oldIdIRI, IRI modelIRI, String body, LoginSession login) {
     
                    /* Remove old graph and add update references */
                    /* TODO: Not allowed if model is draft!?*/
                    boolean success = JerseyFusekiClient.graphIsUpdatedToTheService(idIRI.toString(), body, services.getCoreReadWriteAddress());
                    
                    if (!success) {
                        logger.log(Level.WARNING, "Unexpected: ID not changed: "+idIRI.toString());
                        return null;
                    } 
                    
                    UUID provUUID = UUID.randomUUID();
                   
                    GraphManager.removeGraph(oldIdIRI);
                    GraphManager.renameID(oldIdIRI,idIRI);
                    GraphManager.updateReferencesInPositionGraph(modelIRI, oldIdIRI, idIRI);
                    
                    if(ProvenanceManager.getProvMode()) {
                        ProvenanceManager.renameID(oldIdIRI.toString(),idIRI.toString());
                        ProvenanceManager.createProvenanceGraph(idIRI.toString(), body, login.getEmail(), provUUID);
                    }
                    
                    return provUUID;
        
    }
    
    
    
    public static UUID updateClass(String id, String model, String body, LoginSession login) {
        
        UUID provUUID = UUID.randomUUID();
        
            
        /* Overwrite existing graph */ 
        boolean success = JerseyFusekiClient.graphIsUpdatedToTheService(id, body, services.getCoreReadWriteAddress());
            

       if (!success) {
           /* TODO: Create prov events from failed updates? */
           logger.log(Level.WARNING, "Unexpected: Not updated: "+id);
           return null;
       } 
       
       ConceptMapper.addConceptFromReferencedResource(model,id);

       GraphManager.updateModifyDates(id);
            
        /* If update is successfull create new prov entity */ 
        if(ProvenanceManager.getProvMode()) {
            ProvenanceManager.createProvenanceGraph(id, body, login.getEmail(), provUUID); 
        }
           
        GraphManager.createExportGraphInRunnable(model);
        
        return provUUID;
        
    }
    
    
}
