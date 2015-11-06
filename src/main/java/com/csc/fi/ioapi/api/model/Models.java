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
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("model")
@Api(value = "/model", description = "Operations about data")
public class Models {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
   
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
          @QueryParam("id") String id,
          @ApiParam(value = "group")
          @QueryParam("group") String group) {

      ResponseBuilder rb;
      
      try {
          
          if(group==null || group.equals("undefined")) {
        
            Client client = Client.create();

            if(id==null || id.equals("undefined") || id.equals("default")) id = "urn:csc:iow:sd";
            
            WebResource webResource = client.resource(services.getCoreReadAddress())
                                      .queryParam("graph", id);

            Builder builder = webResource.accept("application/ld+json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Models.class.getName()).log(Level.INFO, response.getStatus()+" from SERVICE "+services.getCoreReadAddress()+" and GRAPH "+id);
               return Response.status(response.getStatus()).entity("{}").build();
            }
            
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();
       
     }
     else { 
              
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            /* IF group parameter is available list of core vocabularies is created */
           queryString = "CONSTRUCT { ?graphName rdfs:label ?label . ?graphName dcterms:identifier ?g . ?graphName dcterms:isPartOf ?group . ?graphName a sd:NamedGraph . } WHERE { ?graph sd:name ?graphName . ?graph a sd:NamedGraph ; dcterms:isPartOf ?group . GRAPH ?graphName {  ?g a owl:Ontology . ?g rdfs:label ?label }}"; 
           pss.setIri("group", group);    


            pss.setCommandText(queryString);
           
            Logger.getLogger(Models.class.getName()).log(Level.INFO, pss.toString());
          
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
            
     }
       
           return rb.build();
           
      } catch(UniformInterfaceException | ClientHandlerException ex) {
          Logger.getLogger(Models.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{}").build();
      }

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
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "Updated model in application/ld+json", required = true) 
                String body, 
          @ApiParam(value = "Model ID", required = true) 
                @QueryParam("id") 
                String graph,
          @Context HttpServletRequest request) {
      
       if(graph.equals("default") || graph.equals("undefined")) {
            return Response.status(403).build();
       } 
 
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(graph))
            return Response.status(401).build();
        
       try {

           String service = services.getCoreReadWriteAddress();
           
           ServiceDescriptionManager.updateGraphDescription(graph);
          
            Client client = Client.create();

            WebResource webResource = client.resource(service).queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Models.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(Models.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Models.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  @PUT
  @ApiOperation(value = "Create new graph and update service description", notes = "PUT Body should be json-ld")
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
          @QueryParam("id") 
                String graph,
          @ApiParam(value = "Group", required = true) 
          @QueryParam("group") 
                String group,
          @Context HttpServletRequest request) {
      
       if(graph.equals("default")) {
           return Response.status(403).build();
       }
             
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditGroup(group))
            return Response.status(401).build();
        
        /* TODO: CHECK IF GRAPH ALREADY EXISTS */ 
        
       try {
 
           if(!graph.equals("undefined")) {
               ServiceDescriptionManager.createGraphDescription(graph, group, login.getEmail());
           }

            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Models.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            Logger.getLogger(Models.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
           
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Models.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
}
