/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("modelPositions")
@Api(tags = {"Model"}, description = "Operations about coordinates")
public class ModelPositions {
  
    @Context ServletContext context;
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ModelPositions.class.getName());
   
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get model from service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
          @ApiParam(value = "Graph id", defaultValue="default") 
          @QueryParam("model") String model) {
  
      return JerseyClient.getNonEmptyGraphResponseFromService(model+"#PositionGraph", services.getCoreReadAddress(), "application/ld+json", false);
  
  }
   
  @PUT
  @ApiOperation(value = "Updates model coordinates", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response putJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) 
                String body, 
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {
      
        if(model.equals("default")) {
            return JerseyResponseManager.invalidIRI();
        }
        
       IRI modelIRI;
       
            try {
                modelIRI = IDManager.constructIRI(model);
            } catch (IRIException e) {
                logger.log(Level.WARNING, "GRAPH ID is invalid IRI!");
                return JerseyResponseManager.invalidIRI();
            } 
            
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn()) {
            return JerseyResponseManager.unauthorized();
        }

        DataModel checkModel = new DataModel(modelIRI);

        if(!login.hasRightToEditModel(checkModel.getOrganizations())) {
            return JerseyResponseManager.unauthorized();
        }

            Model newPositions = ModelManager.createJenaModelFromJSONLDString(body);

            if(newPositions.size()<1) {
                return JerseyResponseManager.invalidParameter();
            }

            JenaClient.putModelToCore(model+"#PositionGraph",newPositions);

            return JerseyResponseManager.okEmptyContent();

  }
  
}
