package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/freeID")
@Tag(name = "Resource")
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
    @Operation(description = "Returns true if ID is valid and not in use")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "False or True response")
    })
    public Response getFreeId(@Parameter(description = "ID", required = true) @QueryParam("id") String id) {

        if (idManager.isInvalid(id)) {
            return jerseyResponseManager.sendBoolean(false);
        }

        return jerseyResponseManager.sendBoolean(!graphManager.isExistingGraph(id));
    }
}
