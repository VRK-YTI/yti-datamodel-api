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
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.GraphManager;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("class")
@Api(value = "/class", description = "Operations about property")
public class Class {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    
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
                queryString = "CONSTRUCT { ?s ?p ?o . } WHERE { GRAPH ?class { ?s ?p ?o . } }";
               // queryString = "CONSTRUCT { ?class a sh:ShapeClass . ?class ?p ?o . ?class sh:property ?prop . ?prop ?pp ?po . ?class rdfs:isDefinedBy ?library . } WHERE { GRAPH ?library { ?library iow:classes ?class . ?class a sh:ShapeClass . ?class ?p ?o . ?class sh:property ?prop . ?prop ?pp ?po .  }}"; 
                pss.setIri("class", id);
            }
            else {
                queryString = "CONSTRUCT { ?class a sh:ShapeClass . ?class rdfs:label ?label . ?class a ?type . ?class rdfs:isDefinedBy ?source . } WHERE { VALUES ?rel {dcterms:hasPart iow:classes} ?library ?rel ?class . GRAPH ?graph { ?class a sh:ShapeClass . ?class rdfs:label ?label . ?class a ?type . ?class rdfs:isDefinedBy ?source . } }"; 
            } 
            
             if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
             }
            

            pss.setCommandText(queryString);
            Logger.getLogger(Class.class.getName()).log(Level.INFO, pss.toString()); 
         
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreSparqlAddress())
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
 
  
  @POST
  @ApiOperation(value = "Create new class to certain model OR add reference from existing class to another model", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "New graph in application/ld+json", required = false) 
                String body, 
          @ApiParam(value = "Class ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {
      
    try {
               
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEdit(model))
            return Response.status(401).build();
        
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).build();
        }
        
        if(body!=null) {
           Client client = Client.create();
           
           WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", id);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Class.class.getName()).log(Level.WARNING, id+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }
            
            GraphManager.insertNewGraphReferenceToModel(idIRI, modelIRI);
            
        } else {
             /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
            if(id.startsWith(model)) {
                // Selfreferences not allowed
                Response.status(403).build();
            } else {
                GraphManager.insertExistingGraphReferenceToModel(idIRI, modelIRI);
            }
        }
            Logger.getLogger(Class.class.getName()).log(Level.INFO, id+" updated sucessfully!");
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  @PUT
  @ApiOperation(value = "Create new class to certain model", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Class ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {
      
    try {
               
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEdit(model))
            return Response.status(401).build();
        
         if(!id.startsWith(model)) {
            Logger.getLogger(Class.class.getName()).log(Level.WARNING, id+" ID must start with "+model);
            return Response.status(403).build();
         }
 
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).build();
        }
          
           Client client = Client.create();
           
           WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", id);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Class.class.getName()).log(Level.WARNING, id+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }
            
            GraphManager.insertNewGraphReferenceToModel(idIRI, modelIRI);
            
            Logger.getLogger(Class.class.getName()).log(Level.INFO, id+" updated sucessfully!");
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  
  
  
  
  
}
