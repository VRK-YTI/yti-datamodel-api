package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.RHPUsersManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.*;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Api(tags = {"Users"}, description = "User requests")
@Path("userRequest")
public class UserRequest {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final RHPUsersManager rhpUsersManager;

    UserRequest(AuthenticatedUserProvider authenticatedUserProvider,
                RHPUsersManager rhpUsersManager) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.rhpUsersManager = rhpUsersManager;
    }

    @GET
    @ApiOperation(value = "Get user requests")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of user objects")
    })
    @Produces("application/json")
    public Response getUserRequests() {

        YtiUser user = authenticatedUserProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        return Response.status(Response.Status.OK)
                .entity(rhpUsersManager.getUserRequests(user.getEmail()))
                .build();
    }

    @POST
    @ApiOperation(value = "Send user request")
    public Response sendUserRequests(@ApiParam(value = "Organization ID", required = true)
                                     @QueryParam("organizationId") String organizationId) {

        YtiUser user = authenticatedUserProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        rhpUsersManager.sendUserRequests(user.getEmail(), organizationId);

        return Response.status(Response.Status.OK).build();
    }
}