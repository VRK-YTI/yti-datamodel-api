/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import com.csc.fi.ioapi.api.genericapi.Data;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ProvenanceManager;
import com.csc.fi.ioapi.utils.QueryLibrary;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("modelPositions")
@Api(value = "/modelPositions", description = "Operations about coordinates")
public class ModelPositions {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
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
  
      return JerseyFusekiClient.getGraphResponseFromService(model+"#PositionGraph",services.getCoreReadAddress(), ContentType.create("application/ld+json"), false);
  
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
            return Response.status(403).entity("{\"errorMessage\":\"Invalid id\"}").build();
        }
        
       IRI graphIRI;
       
            try {
                IRIFactory iri = IRIFactory.iriImplementation();
                graphIRI = iri.construct(model+"#PositionGraph");
            } catch (IRIException e) {
                logger.log(Level.WARNING, "GRAPH ID is invalid IRI!");
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            } 
            
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
            return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
           
            String service = services.getCoreReadWriteAddress();
        
            ClientResponse response = JerseyFusekiClient.putGraphToTheService(model+"#PositionGraph", body, service);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ModelPositions.class.getName()).log(Level.WARNING, model+" coordinates were not saved! Status "+response.getStatus());
               return Response.status(response.getStatus()).entity("{\"errorMessage\":\"Resource was not created\"}").build();
            }
                          
            Logger.getLogger(ModelPositions.class.getName()).log(Level.INFO, model+" coordinates updated sucessfully!");
          
            GraphManager.createExportGraphInRunnable(model);
            
            return Response.status(204).entity("{}").build();

  }
  
}
