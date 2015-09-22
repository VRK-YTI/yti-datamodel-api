package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malonen
 */
import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("class")
@Api(value = "/class", description = "Operations about property")
public class Class {

    @Context ServletContext context;
    
    public String ModelSparqlDataEndpoint() {
       return Endpoint.getEndpoint()+"/core/sparql";
    }
    

  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get property from model", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
          @ApiParam(value = "Class id")
          @QueryParam("id") String id,
          @ApiParam(value = "Model id")
          @QueryParam("model") String model) {

      ResponseBuilder rb;
      
      try {                   
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

  
            if(id!=null && !id.equals("undefined")) {
                queryString = "CONSTRUCT { ?class a sh:ShapeClass . ?class ?p ?o . ?class sh:property ?prop . ?prop ?pp ?po . ?class rdfs:isDefinedBy ?library . } WHERE { GRAPH ?library { ?class a sh:ShapeClass . ?class ?p ?o . ?class sh:property ?prop . ?prop ?pp ?po . ?library iow:classes ?class . }}"; 
                pss.setIri("class", id);
            }
            else {
                queryString = "CONSTRUCT { ?class a sh:ShapeClass . ?class rdfs:label ?label . ?class rdfs:isDefinedBy ?library . } WHERE { GRAPH ?library {?class a sh:ShapeClass . ?class rdfs:label ?label . ?library iow:classes ?class . }}"; 
            } 
            
             if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
             }
            

            pss.setCommandText(queryString);
            Logger.getLogger(Class.class.getName()).log(Level.INFO, pss.toString()); 
         
            Client client = Client.create();

            WebResource webResource = client.resource(ModelSparqlDataEndpoint())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();
           
                  
      } catch(UniformInterfaceException | ClientHandlerException ex) {
          Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{}").build();
      }

  }
   
}
