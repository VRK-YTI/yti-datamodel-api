/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.index.FrameManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jkesanie
 */

@Component
@Path("v1/framedGraphs")
@Api(tags = { "Framed graphs cache" }, description = "Operations for working with framing cache")
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
    @ApiOperation(value = "Get and cache framed model for visualization", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model URI supplied"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "Graph id")
        @QueryParam("graph") String graph) {

        /* Check that URI is valid */
        if (idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        }
        try {
            Date lastModified = graphManager.lastModified(graph);
            String frame = frameManager.getCachedClassVisualizationFrame(graph, lastModified);

            return Response.ok(frame, "application/json").build();
        } catch (NotFoundException fex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }

    }
}
