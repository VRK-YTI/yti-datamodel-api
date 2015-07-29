/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.genericapi;

import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.update.UpdateException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
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
@Path("sparql")
@Api(value = "/sparql", description = "Edit resources")
public class Sparql {

    @Context ServletContext context;
 
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
          @ApiParam(value = "Requested service", required=true) @QueryParam("service") String service, 
          @ApiParam(value = "Sparql query", required = true) 
                String body, @ApiParam(value = "Sparql query", required = true) 
          @QueryParam("graph") 
                String graph) {
       
        String query = LDHelper.prefix + body;
       
        System.out.println(service);
        System.out.println(graph);
        System.out.println(query);
       
        try { 
        UpdateRequest queryObj=UpdateFactory.create(query);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,service);
        qexec.execute();
        
        } catch (  UpdateException ex) {
           return Response.status(400).build();
           }
          catch (  QueryParseException ex) {
            return Response.status(400).build();
           }
   
         return Response.status(200).build();
        
  }
  
  
  
}
