/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.OPHCodeServer;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("codeValues")
@Api(value = "/codeValues", description = "Get codevalues with ID")
public class Codes {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
   
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get code values with id", notes = "Codes will be loaded to TEMP database when used the first time")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "codes"),
      @ApiResponse(code = 406, message = "code not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response concept(
          @ApiParam(value = "uri", required = true) 
          @QueryParam("uri") String uri) {
   
          OPHCodeServer codeServer = new OPHCodeServer(uri, false);
          
          if(!codeServer.status) {
               boolean codeStatus = codeServer.updateCodes(uri);
               if(!codeStatus) return JerseyResponseManager.notAcceptable();
          } 
      
           return JerseyJsonLDClient.getGraphResponseFromService(uri, services.getSchemesReadAddress());

  }
  
  @PUT
  @ApiOperation(value = "Get code values with id", notes = "Update certain code to temp database")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "codes"),
      @ApiResponse(code = 406, message = "code not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response updateCodes(
          @ApiParam(value = "uri", required = true) 
          @QueryParam("uri") String uri) {
   
            ResponseBuilder rb;

            OPHCodeServer codeServer = new OPHCodeServer();
       
            boolean status = codeServer.updateCodes(uri);
            
            rb = Response.status(Status.OK);
            
            if(status) return rb.entity("{}").build();
            else return JerseyResponseManager.notAcceptable();
          
  }
  
  
}
