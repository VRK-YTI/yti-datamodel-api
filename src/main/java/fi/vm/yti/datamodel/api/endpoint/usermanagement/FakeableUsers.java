package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.RHPUsersManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Api(tags = { "Users" }, description = "Get fakeable users")
@Path("fakeableUsers")
public class FakeableUsers {

    private final RHPUsersManager rhpUsersManager;

    @Autowired
    FakeableUsers(RHPUsersManager rhpUsersManager) {
        this.rhpUsersManager = rhpUsersManager;
    }

    @GET
    @ApiOperation(value = "Get fakeable users")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of user objects")
    })
    @Produces("application/json")
    public Response getFakeableUsers() {

        return Response.status(Response.Status.OK)
            .entity(rhpUsersManager.getFakeableUsers())
            .build();
    }
}
