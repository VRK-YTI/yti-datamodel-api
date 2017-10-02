package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import fi.vm.yti.datamodel.api.config.LoginSession;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("loginstatus")
@Api(tags = {"Login"}, description = "Check if user is logged in")
public class LoginStatus {

    @Context
    ServletContext context;

    @GET
    @ApiOperation(value = "Get status")
    @Produces("application/json")
    public boolean getStatus(@Context HttpServletRequest request) {
        return new LoginSession(request.getSession()).isLoggedIn();
    }
}
