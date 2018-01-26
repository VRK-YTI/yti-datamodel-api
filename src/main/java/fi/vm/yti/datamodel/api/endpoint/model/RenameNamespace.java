/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.NamespaceManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Deprecated
@Path("renameNamespace")
@Api(tags = {"Admin"}, description = "HAZARD operation")
public class RenameNamespace {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(RenameNamespace.class.getName());
   
  @PUT
  @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
                @ApiParam(value = "Model ID") 
                @QueryParam("modelID") 
                String modelID,
                @ApiParam(value = "New model ID") 
                @QueryParam("newModelID") 
                String newModelID,
                @Context HttpServletRequest request) {
      
      
       if(modelID==null || modelID.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
       if(newModelID==null || newModelID.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        if(!(login.isLoggedIn() && login.isSuperAdmin())) {
            return JerseyResponseManager.unauthorized();
        }
        
        if(!GraphManager.isExistingGraph(modelID)) {
            return JerseyResponseManager.invalidIRI();
        }
        
       NamespaceManager.renameNamespace(modelID,newModelID);
        
       return JerseyResponseManager.okEmptyContent();

  }

  
}
