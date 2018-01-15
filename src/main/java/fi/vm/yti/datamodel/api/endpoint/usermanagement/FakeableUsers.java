package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.utils.RHPUsersManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Api(tags = {"Users"}, description = "Get fakeable users")
@Path("fakeableUsers")
public class FakeableUsers {

    @Context ServletContext context;

    @GET
    @ApiOperation(value = "Get fakeable users")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of user objects")
    })
    @Produces("application/json")
    public Response getFakeableUsers(@Context HttpServletRequest request) {

        return Response.status(Response.Status.OK)
                .entity(RHPUsersManager.getFakeableUsers())
                .build();
    }
}
