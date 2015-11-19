package com.csc.fi.ioapi.api.model;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.LDHelper;
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
import javax.ws.rs.DELETE;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * @author malonen
 */
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("predicate")
@Api(value = "/predicate", description = "Operations about property")
public class Predicate {

    private static final Logger logger = Logger.getLogger(Predicate.class.getName());

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
          @ApiParam(value = "Property id")
          @QueryParam("id") String id,
          @ApiParam(value = "Model id")
          @QueryParam("model") String model) {

      ResponseBuilder rb;
          
       try {
            
          Client client = Client.create();
          
          if(id==null || id.equals("undefined") || id.equals("default")) {
          
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            queryString = "CONSTRUCT { ?property rdfs:label ?label . ?property a ?type . ?property rdfs:isDefinedBy ?source . ?source rdfs:label ?sourceLabel . ?property dcterms:modified ?date . } WHERE { ?library dcterms:hasPart ?property . GRAPH ?graph { ?property rdfs:label ?label . VALUES ?type { owl:ObjectProperty owl:DatatypeProperty } ?property a ?type . ?property rdfs:isDefinedBy ?source . ?property dcterms:modified ?date .  } GRAPH ?source { ?source rdfs:label ?sourceLabel . }}"; 
       
             if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
             }
            
            pss.setCommandText(queryString);
         
            WebResource webResource = client.resource(services.getCoreSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
            
          } else {
              
            WebResource webResource = client.resource(services.getCoreReadAddress())
                                      .queryParam("graph", id);

            Builder builder = webResource.accept("application/ld+json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                return Response.status(response.getStatus()).entity("{}").build();
            }
            
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
          }
         
        return rb.build();
           
      } catch(UniformInterfaceException | ClientHandlerException ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{}").build();
      }

  }
   
  
  @POST
  @ApiOperation(value = "Create new property to certain model OR add reference from existing property to another model", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Property ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "OLD Property ID") 
          @QueryParam("oldid") 
                String oldid,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {
      
    try {
               
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
            return Response.status(401).build();
 
         String graphID = id;
                
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI,oldIdIRI = null; 
        
       /* Check that URIs are valid */
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
            /* If oldid exists */
            if(oldid!=null && !oldid.equals("undefined")) {
                if(oldid.equals(id)) {
                  /* id and oldid cant be the same */
                  return Response.status(403).build();
                }
                oldIdIRI = iriFactory.construct(oldid);
            }
        }
        catch (IRIException e) {
            logger.log(Level.SEVERE, e.toString());
            return Response.status(403).build();
        }
        
        if(isNotEmpty(body)) {
            
            /* Rename ID */
            if(oldIdIRI!=null) {
                /* Prevent overwriting existing resources */
                if(GraphManager.isExistingGraph(idIRI)) {
                    logger.log(Level.WARNING, idIRI+" is existing graph!");
                    return Response.status(403).build();
                }
                else {
                    /* Remove old graph and add update references */
                    GraphManager.removeGraph(oldIdIRI);
                    GraphManager.renameID(oldIdIRI,idIRI);
                }
            }
            
        /* Create new graph with new id */ 
        Client client = Client.create();

        WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                  .queryParam("graph", graphID);

        WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
        ClientResponse response = builder.put(ClientResponse.class,body);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           logger.log(Level.WARNING, id + " was not updated! Status " + response.getStatus());
           return Response.status(response.getStatus()).build();
        }
            
        } else {
             /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
            if(id.startsWith(model)) {
                // Selfreferences not allowed
                Response.status(403).build();
            } else {
                GraphManager.insertExistingGraphReferenceToModel(idIRI, modelIRI);
            }
        }
        
        logger.log(Level.INFO, id + " updated sucessfully!");
        return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        logger.log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  @PUT
  @ApiOperation(value = "Create new property to certain model", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Property ID", required = true) 
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

            if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
                return Response.status(401).build();

            if(!id.startsWith(model))
                return Response.status(403).build();

            IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
            IRI modelIRI,idIRI;
            try {
                modelIRI = iriFactory.construct(model);
                idIRI = iriFactory.construct(id);
            }
            catch (IRIException e) {
                return Response.status(403).build();
            }

            /* Prevent overwriting existing predicate */ 
            if(GraphManager.isExistingGraph(idIRI)) {
               logger.log(Level.WARNING, idIRI+" is existing predicate!");
               return Response.status(403).build();
            }
        
           Client client = Client.create();
           
           WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", id);

           WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
           ClientResponse response = builder.put(ClientResponse.class,body);

           if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, id + " was not updated! Status " + response.getStatus());
               return Response.status(response.getStatus()).build();
           }
            
           GraphManager.insertNewGraphReferenceToModel(idIRI, modelIRI);
            
           logger.log(Level.INFO, id + " updated sucessfully!");
            
           return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        logger.log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  @DELETE
  @ApiOperation(value = "Delete predicate graph or reference", notes = "Deletes predicate graph or reference")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph"),
      @ApiResponse(code = 401, message = "Unauthorized")
  })
  public Response deletePredicate(
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") String model,
          @ApiParam(value = "Predicate ID", required = true) 
          @QueryParam("id") String id,
          @Context HttpServletRequest request) {
      
      IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).build();
        }
      
       if(id.equals("default")) {
               return Response.status(403).build();
       }
       
       HttpSession session = request.getSession();

       if(session==null) return Response.status(401).build();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return Response.status(401).build();
       
       /* If Class is defined in the model */
       if(id.startsWith(model)) {

        try {

             Client client = Client.create();
             WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                       .queryParam("graph", id);

             WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
             ClientResponse response = builder.delete(ClientResponse.class);

             if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.WARNING, id+" was not deleted! Status "+response.getStatus());
                return Response.status(response.getStatus()).build();
             }
             
             logger.log(Level.INFO, id+" deleted successfully!");
             return Response.status(204).build();

       } catch(UniformInterfaceException | ClientHandlerException ex) {
             logger.log(Level.WARNING, "Expect the unexpected!", ex);
             return Response.status(400).build();
       }
        
    } else {
        /* If removing referenced predicate */   
         GraphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);  
         return Response.status(204).build();   
       }
  }
  
 
}
