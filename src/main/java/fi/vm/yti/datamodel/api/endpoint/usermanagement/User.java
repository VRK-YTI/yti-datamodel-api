package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/user")
@Api(tags = { "Users" }, description = "Get user")
public class User {

    private final AuthenticatedUserProvider userProvider;

    User(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GET
    @ApiOperation(value = "Get authenticated user")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User object")
    })
    @Produces("application/json")
    public Response getUser() {
        return Response.status(200).entity(userProvider.getUser()).build();
    }
}
