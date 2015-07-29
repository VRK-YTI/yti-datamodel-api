/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.profile;

import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.csc.fi.ioapi.genericapi.Data;
import com.csc.fi.ioapi.model.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("profile-edit")
@Api(value = "/profile-edit", description = "Edit resources")
public class ProfileEdit {

    @Context ServletContext context;
        
    public String ProfileSparqlUpdateEndpoint() {
       return Endpoint.getEndpoint()+"/profile/update";
    }
    
    public String ProfileDataEndpoint() {
      return Endpoint.getEndpoint()+"/profile/data";
    }
        
    /**
     * Replaces Graph in given service
     * @returns empty Response
     */
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
      
       if(graph.equals("default")) {
            return Response.status(403).build();
       } 
       try {

           String service = ProfileDataEndpoint();

           if(!(graph.equals("undefined") || graph.equals("default"))) {
               ServiceDescriptionManager.updateGraphDescription(ProfileSparqlUpdateEndpoint(), graph);
           }

            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatus() != 204) {
               Logger.getLogger(ProfileEdit.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(ProfileEdit.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  @PUT
  @ApiOperation(value = "Create new graph and update service description", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response putJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) 
                String body, 
          @ApiParam(value = "Graph to overwrite", required = true) 
          @QueryParam("graph") 
                String graph) {
      
       if(graph.equals("default")) {
           return Response.status(403).build();
       }
       try {

            String service = ProfileDataEndpoint();

           if(!(graph.equals("undefined") || graph.equals("default"))) {
               ServiceDescriptionManager.createGraphDescription(ProfileSparqlUpdateEndpoint(), graph);
           }

            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatus() != 204) {
               Logger.getLogger(ProfileEdit.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(ProfileEdit.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
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
      
       if(graph.equals("default")) {
           return Response.status(403).build();
       }

       try {

            String service = ProfileDataEndpoint();

            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.delete(ClientResponse.class);

            if (response.getStatus() != 204) {
               Logger.getLogger(ProfileEdit.class.getName()).log(Level.WARNING, graph+" was not deleted! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            if(!(graph.equals("undefined") || graph.equals("default"))) {
               ServiceDescriptionManager.deleteGraphDescription(ProfileSparqlUpdateEndpoint(), graph);
           }

            Logger.getLogger(ProfileEdit.class.getName()).log(Level.INFO, graph+" deleted successfully!");
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.status(400).build();
      }
  }
  
  
}
