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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("serviceDescription")
@Api(value = "/serviceDescription", description = "IOW service description")
public class ServiceDescription {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ServiceDescription.class.getName());
  
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get service description", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service description not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json() {

    return JerseyFusekiClient.getGraphResponseFromService("urn:csc:iow:sd",services.getCoreReadAddress());

}
  
}
