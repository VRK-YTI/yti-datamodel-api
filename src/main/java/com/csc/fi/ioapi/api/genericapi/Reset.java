/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.genericapi;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.GroupManager;
import com.csc.fi.ioapi.utils.NamespaceManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("reset")
@Api(value = "/reset", description = "DROP ALL and Recover")
public class Reset {

    @Context ServletContext context;

    @GET
    @ApiOperation(value = "Log user out", notes = "Removes session used by API")
      @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Logged out"),
      @ApiResponse(code = 400, message = "Not logged in")
  })
    public Response drop(@Context HttpServletRequest request) {
        Response.ResponseBuilder rb;
              
        if(ApplicationProperties.getDebugMode()) {
            GraphManager.deleteGraphs();
            GraphManager.createDefaultGraph();
            GroupManager.createDefaultGroups();
            NamespaceManager.addDefaultNamespacesToCore();
        }
        
        rb = Response.status(Response.Status.OK);
     
        return rb.build();
        
    }
    
}
