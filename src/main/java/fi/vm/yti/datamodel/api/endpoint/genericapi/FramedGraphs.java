/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.index.FrameManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * TODO: Remove this class
 */
@Component
@Path("v1/framedGraphs")
@Tag(name = "Frame")
public class FramedGraphs {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final FrameManager frameManager;
    private final GraphManager graphManager;
    Logger logger = LoggerFactory.getLogger(FramedGraphs.class);

    @Autowired
    FramedGraphs(IDManager idManager,
                 JerseyResponseManager jerseyResponseManager,
                 FrameManager frameManager,
                 GraphManager graphManager) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.frameManager = frameManager;
        this.graphManager = graphManager;
    }

    @GET
    @Produces("application/json")
    @Operation(description = "Get and cache framed model for visualization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model URI supplied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getFramedGraphs(
        @Parameter(description = "Graph id")
        @QueryParam("graph") String graph) {

        /* Check that URI is valid */
        if (idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        }
        try {
            Date contentModified = graphManager.modelContentModified(graph);
            String frame = ""; // frameManager.getCachedClassVisualizationFrame(graph, contentModified);

            return Response.ok(frame, "application/json").build();
        } catch (NotFoundException fex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }

    }
}
