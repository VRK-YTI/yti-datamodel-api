/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.genericapi;

import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
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
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("sparql")
@Api(value = "/sparql", description = "Edit resources")
public class Sparql {

    @Context ServletContext context;

  @GET
  @Consumes("application/sparql-query")
  @Produces("application/sparql-results+json")
  @ApiOperation(value = "Sparql query to given service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Query parse error"),
      @ApiResponse(code = 500, message = "Query exception"),
      @ApiResponse(code = 200, message = "OK")
  })
  public Response sparql(@ApiParam(value = "Requested service", required=true) @QueryParam("endpoint") String endpoint, @ApiParam(value = "Requested resource", defaultValue="default") @QueryParam("graph") String graph, @ApiParam(value = "SPARQL Query", required = true) @QueryParam("query") String queryString) {      

         Logger.getLogger(Sparql.class.getName()).log(Level.INFO, "Querying graph: "+graph+" in endpoint: "+endpoint+" with query: "+queryString);
         
         Query query;
         
         try{
             query = QueryFactory.create(queryString);
         } catch(QueryParseException ex) {
             return Response.status(400).build();
         }
         
         QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
         
         try {
     
         OutputStream outs = new ByteArrayOutputStream();
         ResultSet results = qexec.execSelect();
         ResultSetFormatter.outputAsJSON(outs,results);
         
         return  Response
            .ok(outs.toString(), "application/sparql-results+json")
            .build();
                 
         } catch(QueryException ex) {
            return Response.status(500).build();
         } finally {
            qexec.close();
         }
         


  }
  
      /**
     * SPARQL Update
     * NOT USUALLY USED TROUGH THIS API
     */
  
  /*
  @POST
  @ApiOperation(value = "Sends SPARQL Update query to given service", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  
  public Response sparqlUpdate(
          @ApiParam(value = "Requested service", required=true) @QueryParam("service") String service, 
          @ApiParam(value = "Sparql query", required = true) 
                String body, @ApiParam(value = "Sparql query", required = true) 
          @QueryParam("graph") 
                String graph) {
       
        String query = LDHelper.prefix + body;
       
        Logger.getLogger(Sparql.class.getName()).log(Level.INFO, "Updating graph: "+graph+" in service: "+service+" with query: "+query);
              
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
  */
  
}
