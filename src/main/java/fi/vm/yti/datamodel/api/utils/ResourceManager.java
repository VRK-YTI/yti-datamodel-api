/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.config.EndpointServices;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.iri.IRI;

/**
 *
 * @author malonen
 */
@Deprecated
public class ResourceManager {
    
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    /**
     * Creates new resource to the model
     * @param id ID of the resource
     * @param model ID of the model
     * @param body Resource as json-ld string
     * @param login Login session
     * @return UUID of the resource
     */
    public static UUID putNewResource(String id, String model, String body, LoginSession login) {
        
            /* Create new graph */ 
           boolean success = JerseyJsonLDClient.graphIsUpdatedToTheService(id, body, services.getCoreReadWriteAddress());

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
            
           // ConceptMapper.addConceptFromReferencedResource(model,id);
            
            GraphManager.insertNewGraphReferenceToExportGraph(id,model);
            JerseyJsonLDClient.postGraphToTheService(model+"#ExportGraph", body, services.getCoreReadWriteAddress());
           
            return provUUID;
    }

    /**
     * Renames resource id
     * @param idIRI New id
     * @param oldIdIRI Old id
     * @param modelIRI Model id
     * @param body Resource as json-ld string
     * @param login Login session
     * @return UUID of the resource
     */
    public static UUID updateResourceWithNewId(IRI idIRI, IRI oldIdIRI, IRI modelIRI, String body, LoginSession login) {
     
                    /* Remove old graph and add update references */
                    /* TODO: Not allowed if model is draft!?*/
                    boolean success = JerseyJsonLDClient.graphIsUpdatedToTheService(idIRI.toString(), body, services.getCoreReadWriteAddress());
                    
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
                        ProvenanceManager.createProvenanceGraph(idIRI.toString(), body, login.getEmail(), "urn:uuid:"+provUUID.toString());
                    }
                    
                    return provUUID;
        
    }


    public static UUID updateResource(String id, String model, String body, LoginSession login) {
        
        UUID provUUID = UUID.randomUUID();
        
            
        /* Overwrite existing graph */ 
        boolean success = JerseyJsonLDClient.graphIsUpdatedToTheService(id, body, services.getCoreReadWriteAddress());
            

       if (!success) {
           /* TODO: Create prov events from failed updates? */
           logger.log(Level.WARNING, "Unexpected: Not updated: "+id);
           return null;
       } 
       
     //  ConceptMapper.addConceptFromReferencedResource(model,id);

       GraphManager.updateModifyDates(id);
            
        /* If update is successfull create new prov entity */ 
        if(ProvenanceManager.getProvMode()) {
            ProvenanceManager.createProvenanceGraph(id, body, login.getEmail(), "urn:uuid:"+provUUID.toString());
        }
           
        GraphManager.createExportGraphInRunnable(model);
        
        return provUUID;
        
    }
    
    
}
