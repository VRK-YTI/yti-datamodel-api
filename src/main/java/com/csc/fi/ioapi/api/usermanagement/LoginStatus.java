package com.csc.fi.ioapi.api.usermanagement;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.csc.fi.ioapi.config.LoginSession;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("loginstatus")
@Api(value = "/loginstatus", description = "Check if user is logged in")
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
