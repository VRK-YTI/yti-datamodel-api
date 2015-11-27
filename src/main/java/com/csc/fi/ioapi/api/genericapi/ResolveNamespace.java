package com.csc.fi.ioapi.api.genericapi;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.NamespaceResolver;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.ws.rs.QueryParam;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("resolveNamespace")
@Api(value = "/resolveNamespace", description = "Import for external references")
public class ResolveNamespace {

   @Context ServletContext context;
   EndpointServices services = new EndpointServices();
   
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get service description", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service description not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(@ApiParam(value = "Namespace", required = true) @QueryParam("namespace") String namespace)  {

        if(NamespaceResolver.resolveNamespace(namespace)) {
             return JerseyFusekiClient.getGraphResponseFromService(namespace,services.getImportsReadAddress());
        } else {
            return Response.serverError().entity("{\"errorMessage\":\"Namespace could not be resolved\"}").build();
        }
}
  
   
}
