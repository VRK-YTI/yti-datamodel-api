package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.RHPUsersManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Tag(name = "Users")
@Path("v1/userRequest")
public class UserRequest {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final RHPUsersManager rhpUsersManager;

    UserRequest(AuthenticatedUserProvider authenticatedUserProvider,
                RHPUsersManager rhpUsersManager) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.rhpUsersManager = rhpUsersManager;
    }

    @GET
    @Operation(description = "Get user requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of user objects")
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
    @Operation(description = "Send user request")
    public Response sendUserRequests(@Parameter(description = "Organization ID", required = true)
                                     @QueryParam("organizationId") String organizationId) {

        YtiUser user = authenticatedUserProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        rhpUsersManager.sendUserRequests(user.getEmail(), organizationId);

        return Response.status(Response.Status.OK).build();
    }
}
