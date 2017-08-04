package com.csc.fi.ioapi.api.genericapi;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import javax.ws.rs.core.HttpHeaders;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.HeaderParam;
import org.apache.jena.atlas.web.ContentType;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("serviceDescription")
@Api(tags = {"Model"}, description = "IOW service description")
public class ServiceDescription {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ServiceDescription.class.getName());
  
  @GET
  //@Produces("application/ld+json")
  @ApiOperation(value = "Get service description", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service description not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(@HeaderParam("Accept") String accept) {
    return JerseyJsonLDClient.getGraphResponseFromService("urn:csc:iow:sd",services.getCoreReadAddress(), ContentType.create(accept), false);
}
  
}
