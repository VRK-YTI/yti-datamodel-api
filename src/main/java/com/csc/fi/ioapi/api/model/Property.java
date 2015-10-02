package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("property")
@Api(value = "/property", description = "Operations about property")
public class Property {

    @Context ServletContext context;
    
    public String ModelSparqlDataEndpoint() {
       return ApplicationProperties.getEndpoint()+"/core/sparql";
    }
    
    public String ModelSparqlUpdateEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/update";
    }
    
    public String ModelUpdateDataEndpoint() {
       return ApplicationProperties.getEndpoint()+"/search/data";
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
          @ApiParam(value = "Property id")
          @QueryParam("id") String id,
          @ApiParam(value = "Model id")
          @QueryParam("model") String model) {

      ResponseBuilder rb;
      
      try {                   
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            if(id!=null && !id.equals("undefined")) {
                queryString = "CONSTRUCT { ?s ?p ?o . } WHERE { GRAPH ?property { ?s ?p ?o . } }";             
                //   queryString = "CONSTRUCT { ?property ?p ?o . ?property rdfs:isDefinedBy ?library } WHERE { VALUES ?rel { iow:associations iow:attributes } ?library ?rel ?property . GRAPH ?library { ?property ?p ?o . }}"; 
                pss.setIri("property", id);
            } else {
                queryString = "CONSTRUCT { ?property rdfs:label ?label . ?property a ?type . ?property rdfs:isDefinedBy ?source . } WHERE { VALUES ?rel {dcterms:hasPart iow:associations iow:attributes} ?library ?rel ?property . GRAPH ?graph { ?property rdfs:label ?label . VALUES ?type { owl:ObjectProperty owl:DatatypeProperty } ?property a ?type . ?property rdfs:isDefinedBy ?source . } }"; 
            }
            
            if(model!=null && !model.equals("undefined")) {
                pss.setIri("library", model);
            }
            
            pss.setCommandText(queryString);
           
            Logger.getLogger(Property.class.getName()).log(Level.INFO, pss.toString());
          
            Client client = Client.create();

            WebResource webResource = client.resource(ModelSparqlDataEndpoint())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();
           
                  
      } catch(UniformInterfaceException | ClientHandlerException ex) {
          Logger.getLogger(Property.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{}").build();
      }

  }
   
  
  @POST
  @ApiOperation(value = "Create new property to certain model OR add reference from existing property to another model", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
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
                String model) {
      
       try {
 
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
           String service = ModelUpdateDataEndpoint();
           Client client = Client.create();
           
           WebResource webResource = client.resource(service)
                                      .queryParam("graph", id);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.post(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Property.class.getName()).log(Level.WARNING, id+" was not updated! Status "+response.getStatus());
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
            Logger.getLogger(Property.class.getName()).log(Level.INFO, id+" updated sucessfully!");
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Property.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  @PUT
  @ApiOperation(value = "Create new property to certain model", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Class ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model) {
      
       try {
           
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
          
           String service = ModelUpdateDataEndpoint();
           Client client = Client.create();
           
           WebResource webResource = client.resource(service)
                                      .queryParam("graph", id);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Property.class.getName()).log(Level.WARNING, id+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }
            
            GraphManager.insertNewGraphReferenceToModel(idIRI, modelIRI);
            
            Logger.getLogger(Property.class.getName()).log(Level.INFO, id+" updated sucessfully!");
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Property.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  
  
  
}
