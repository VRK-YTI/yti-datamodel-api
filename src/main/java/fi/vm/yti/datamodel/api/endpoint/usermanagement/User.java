/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.config.LoginSession;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("user")
@Api(tags = {"Users"}, description = "Get user")
public class User {

    @Context ServletContext context;

    private static final Logger logger = Logger.getLogger(User.class.getName());

    @GET
    @ApiOperation(value = "Get authenticated user")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User object")
    })
    @Produces("application/json")
    public Response getUser(@Context HttpServletRequest request) {

        logger.info("Getting user");

        HttpSession session = request.getSession();
        LoginSession login = new LoginSession(session);

        return Response.status(200).entity(login.getUser()).build();
    }
}
