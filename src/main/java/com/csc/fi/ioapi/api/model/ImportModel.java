package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("importModel")
@Api(value = "/importModel", description = "Import existing model")
public class ImportModel {

    @Context ServletContext context;
    
    public String ModelDataEndpoint() {
       return ApplicationProperties.getEndpoint()+"/core/get";
    }
    
   public String ModelUpdateDataEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/data";
    }
    
    public String ModelSparqlDataEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/sparql";
    }
    
    public String ModelSparqlUpdateEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/update";
    }


  @POST
  @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) 
                String body, @ApiParam(value = "Graph to overwrite", required = true) 
                @QueryParam("graph") 
                String graph) {
      
       if(graph.equals("default") || graph.equals("undefined")) {
            return Response.status(403).build();
       } 
       try {

           String service = ModelUpdateDataEndpoint();
           
           ServiceDescriptionManager.updateGraphDescription(ModelSparqlUpdateEndpoint(), graph);
          
            Client client = Client.create();

            WebResource webResource = client.resource(service).queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(ImportModel.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
            
            GraphManager.createResourceGraphs(graph);

            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  @PUT
  @ApiOperation(value = "Create new graph and update service description", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response putJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) 
                String body, 
          @ApiParam(value = "Graph to overwrite", required = true) 
          @QueryParam("graph") 
                String graph,
          @ApiParam(value = "Group", required = true) 
          @QueryParam("group") 
                String group) {
      
       if(graph.equals("default")) {
           return Response.status(403).build();
       }
       
       try {
 
           String service = ModelUpdateDataEndpoint();

           if(!graph.equals("undefined")) {
               ServiceDescriptionManager.createGraphDescription(ModelSparqlUpdateEndpoint(), graph, group);
           }

            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(ImportModel.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
            
            GraphManager.createResourceGraphs(graph);
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  @DELETE
  @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph")
  })
  public Response deleteJson(
          @ApiParam(value = "Graph to be deleted", required = true) 
          @QueryParam("graph") 
                String graph) {
      
    /*if(graph.equals("default")) {
           return Response.status(403).build();
     }*/

       try {

            String service = ModelUpdateDataEndpoint();

            
            GraphManager.deleteResourceGraphs(graph);
            
            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.delete(ClientResponse.class);

           if(!(graph.equals("undefined") || graph.equals("default"))) {
               ServiceDescriptionManager.deleteGraphDescription(graph);
           }
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, graph+" was not deleted! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }



            Logger.getLogger(ImportModel.class.getName()).log(Level.INFO, graph+" deleted successfully!");
            
           
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
            Logger.getLogger(ImportModel.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.status(400).build();
      }
  }
  
  
  
  
}
