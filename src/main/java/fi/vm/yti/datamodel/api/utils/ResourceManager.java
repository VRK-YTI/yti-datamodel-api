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
    @Deprecated
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
             //  ProvenanceManager.createProvenanceActivity(id, login.getEmail(), body, provUUID);
           }
            
            GraphManager.insertNewGraphReferenceToModel(id, model);
            
            logger.log(Level.INFO, id+" updated sucessfully!");
            
           // ConceptMapper.addConceptFromReferencedResource(model,id);
            
            GraphManager.insertNewGraphReferenceToExportGraph(id,model);
            JerseyJsonLDClient.postGraphToTheService(model+"#ExportGraph", body, services.getCoreReadWriteAddress());
           
            return provUUID;
    }


    
    
}
