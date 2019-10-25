package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/user")
@Tag(name = "Users")
public class User {

    private final AuthenticatedUserProvider userProvider;

    User(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GET
    @Operation(description = "Get authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User object")
    })
    @Produces("application/json")
    public Response getUser() {
        return Response.status(200).entity(userProvider.getUser()).build();
    }
}
