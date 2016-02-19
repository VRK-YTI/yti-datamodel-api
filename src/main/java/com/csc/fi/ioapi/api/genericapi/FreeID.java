package com.csc.fi.ioapi.api.genericapi;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.sun.jersey.api.client.ClientResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.logging.Level;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("freeID")
@Api(value = "/testID", description = "Test if ID is valid and not in use")
public class FreeID {

  @Context ServletContext context;
  EndpointServices services = new EndpointServices();
  private static final Logger logger = Logger.getLogger(FreeID.class.getName());
  
  @GET
  @Produces("application/json")
  @ApiOperation(value = "Get service description", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "False or True response")
  })
  public Response json(@ApiParam(value = "ID", required = true) @QueryParam("id") String id) {
     
      IRI idIRI;
            
            try {
                IRIFactory iri = IRIFactory.semanticWebImplementation();
                idIRI = iri.construct(id);
            } catch(NullPointerException e) {
                return Response.status(Status.OK).entity(false).build();
            } 
            catch (IRIException e) {
                return Response.status(Status.OK).entity(false).build();
            }
            
    return Response.status(Status.OK).entity(!GraphManager.isExistingGraph(idIRI)).build();

}
  
}
