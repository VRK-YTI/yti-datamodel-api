package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("freeID")
@Api(tags = { "Resource" }, description = "Test if ID is valid and not in use")
public class FreeID {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;

    @Autowired
    FreeID(IDManager idManager,
           JerseyResponseManager jerseyResponseManager,
           GraphManager graphManager) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Returns true if ID is valid and not in use", notes = "ID must be valid IRI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "False or True response")
    })
    public Response json(@ApiParam(value = "ID", required = true) @QueryParam("id") String id) {

        if (idManager.isInvalid(id)) {
            return jerseyResponseManager.sendBoolean(false);
        }

        return jerseyResponseManager.sendBoolean(!graphManager.isExistingGraph(id));
    }
}
