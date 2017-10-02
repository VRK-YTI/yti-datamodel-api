/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("logout")
@Api(tags = {"Login"}, description = "logout user")
public class Logout {

    @Context ServletContext context;

    @GET
    @ApiOperation(value = "Log user out", notes = "Removes session used by API")
      @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Logged out"),
      @ApiResponse(code = 400, message = "Not logged in")
  })
    public Response logout(@Context HttpServletRequest request) {
        Response.ResponseBuilder rb;
        
        HttpSession session = request.getSession(false);
        
        if(session==null) 
            rb = Response.status(Response.Status.BAD_REQUEST);
        else {
            session.invalidate();
            rb = Response.status(Response.Status.OK);
        }  
        
        return rb.build();
        
    }


}
