/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.genericapi;

import com.csc.fi.ioapi.api.genericapi.*;
import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
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
import com.sun.jersey.api.uri.UriComponent;
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
@Path("search")
@Api(value = "/search", description = "Search resources")
public class Search {

    @Context ServletContext context;

    public String ModelSparqlDataEndpoint() {
       return Endpoint.getEndpoint()+"/core/sparql";
    }
            
  @GET
  @Consumes("application/sparql-query")
  @Produces("application/ld+json")
  @ApiOperation(value = "Sparql query to given service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Query parse error"),
      @ApiResponse(code = 500, message = "Query exception"),
      @ApiResponse(code = 200, message = "OK")
  })
  public Response sparql(
          @ApiParam(value = "Search in graph", defaultValue="default") @QueryParam("graph") String graph,
          @ApiParam(value = "Searchstring", required = true) @QueryParam("search") String search,
          @ApiParam(value = "Language", required = true) @QueryParam("lang") String lang) {      

      String queryString = "CONSTRUCT {"
              + "?resource rdf:type ?type ."
              + "?resource rdfs:label ?label ."
              + "} WHERE { "
              + "?resource ?p ?o . "
              + "?resource rdf:type ?type ."
              + "?resource rdfs:label ?label . "
              + "FILTER langMatches(lang(?label),'"+lang+"') "
              + "FILTER CONTAINS(LCASE(?o),'"+search.toLowerCase()+"')}";   
      
       ParameterizedSparqlString pss = new ParameterizedSparqlString();
       pss.setNsPrefixes(LDHelper.PREFIX_MAP);
       pss.setCommandText(queryString);
      
       Logger.getLogger(Search.class.getName()).log(Level.INFO, "Searching "+graph+" with query: "+queryString);
      
        ResponseBuilder rb;
              Client client = Client.create();

            WebResource webResource = client.resource(ModelSparqlDataEndpoint())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();    

  }
  
}
