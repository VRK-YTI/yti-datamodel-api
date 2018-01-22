package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.model.YtiUser;
import fi.vm.yti.datamodel.api.utils.RHPUsersManager;
import io.swagger.annotations.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Api(tags = {"Users"}, description = "User requests")
@Path("userRequest")
public class UserRequest {

    @Context
    ServletContext context;

    @GET
    @ApiOperation(value = "Get user requests")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of user objects")
    })
    @Produces("application/json")
    public Response getUserRequests(@Context HttpServletRequest request) {

        YtiUser user = new LoginSession(request.getSession()).getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        return Response.status(Response.Status.OK)
                .entity(RHPUsersManager.getUserRequests(user.getEmail()))
                .build();
    }

    @POST
    @ApiOperation(value = "Send user request")
    public Response sendUserRequests(@ApiParam(value = "Organization ID", required = true)
                                     @QueryParam("organizationId") String organizationId,
                                     @Context HttpServletRequest request) {

        YtiUser user = new LoginSession(request.getSession()).getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        RHPUsersManager.sendUserRequests(user.getEmail(), organizationId);

        return Response.status(Response.Status.OK).build();
    }
}