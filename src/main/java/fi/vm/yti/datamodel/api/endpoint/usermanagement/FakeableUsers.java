package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.RHPUsersManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Tag(name = "Admin")
@Path("v1/fakeableUsers")
public class FakeableUsers {

    private final RHPUsersManager rhpUsersManager;

    @Autowired
    FakeableUsers(RHPUsersManager rhpUsersManager) {
        this.rhpUsersManager = rhpUsersManager;
    }

    @GET
    @Operation(description = "Get fakeable users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of user objects")
    })
    @Produces("application/json")
    public Response getFakeableUsers() {

        return Response.status(Response.Status.OK)
            .entity(rhpUsersManager.getFakeableUsers())
            .build();
    }
}
