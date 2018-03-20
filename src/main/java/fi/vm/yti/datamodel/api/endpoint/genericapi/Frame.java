/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.FrameManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jkesanie
 */

@Path("frame")
public class Frame {
    
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final FrameManager frameManager;

    @Autowired
    Frame(IDManager idManager,
          JerseyResponseManager jerseyResponseManager,
          FrameManager frameManager) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.frameManager = frameManager;
    }
    
    @GET
    @Produces("application/json")
    public Response json(
        @QueryParam("graph") String graph) {
        
        /* Check that URIs are valid */
        if(idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        }
        try {
            String frame = frameManager.getCachedClassVisualizationFrame(graph);            
            return Response.ok(frame, "application/json").build();
        }catch(Exception ex) {
            ex.printStackTrace();
            return Response.serverError().entity(ex.getMessage()).build();
        }
        
    }
}
