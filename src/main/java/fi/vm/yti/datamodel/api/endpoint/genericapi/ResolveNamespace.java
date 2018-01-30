package fi.vm.yti.datamodel.api.endpoint.genericapi;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.utils.JerseyClient;
import fi.vm.yti.datamodel.api.utils.NamespaceResolver;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("resolveNamespace")
@Api(tags = {"Admin"}, description = "Import for external references")
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
  public Response getJson(@ApiParam(value = "Namespace", required = true) @QueryParam("namespace") String namespace)  {

        if(NamespaceResolver.resolveNamespace(namespace,null,false)) {
             return JerseyClient.getGraphResponseFromService(namespace,services.getImportsReadAddress());
        } else {
            return JerseyResponseManager.invalidIRI();
        }
        
}
  
  @POST
  @Produces("application/json")
  @ApiOperation(value = "Get service description", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service description not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(@ApiParam(value = "Namespace", required = true) @QueryParam("namespace") String namespace,
          @ApiParam(value = "Alternative url for the RDF") @QueryParam("altURL") String alternativeURL,
          @ApiParam(value = "Force update", required = true) @QueryParam("force") boolean force)  {

        if(NamespaceResolver.resolveNamespace(namespace,(alternativeURL!=null&&!alternativeURL.equals("undefined")?alternativeURL:null),force)) {
             return JerseyResponseManager.okEmptyContent();
        } else {
            return JerseyResponseManager.invalidIRI();
        }
        
       
        
}
  
   
}
