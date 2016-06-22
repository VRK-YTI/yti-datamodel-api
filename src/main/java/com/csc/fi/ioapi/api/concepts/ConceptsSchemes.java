/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSchemes")
@Api(value = "/conceptSchemes", description = "Available concept schemes in Finto")
public class ConceptsSchemes {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
   
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get available concepts", notes = "Search from finto API & concept temp")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Concepts"),
      @ApiResponse(code = 406, message = "Term not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response vocab() {
           return JerseyFusekiClient.getGraphResponseFromService("urn:csc:schemes", services.getTempConceptReadWriteAddress());
  }
  
  
}
