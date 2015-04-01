/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.usermanagement;

import com.csc.fi.ioapi.genericapi.Data;
import com.csc.fi.ioapi.utils.LDHelper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("users")
@Api(value = "/users", description = "Edit users")
public class User {

    @Context ServletContext context;

    public String userEndpoint() {
       return context.getInitParameter("UserDataEndpoint");
    }
    
    public String userSparqlEndpoint() {
       return context.getInitParameter("UserSparqlEndpoint");
    }
    
    public String userSparqlUpdateEndpoint() {
       return context.getInitParameter("UserSparqlUpdateEndpoint");
    }
     
    @GET
    @ApiOperation(value = "Get user id", notes = "Get user from service")
      @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    @Produces("application/ld+json")
    public Response getUser(@ApiParam(value = "email") @QueryParam("email") String email) {
       
        String queryString = LDHelper.prefix+"CONSTRUCT { ?id a foaf:Person ; foaf:fullName ?name . } WHERE { GRAPH <urn:csc:users> {  ?id a foaf:Person ; foaf:fullName ?name; foaf:mbox "+(email==null||email.equals("undefined")?"?email":"'"+email+"'")+" }}";

        Client client = Client.create();

        WebResource webResource = client.resource(userSparqlEndpoint())
                                  .queryParam("query", UriComponent.encode(queryString,UriComponent.Type.QUERY));

        WebResource.Builder builder = webResource.accept("application/ld+json");

        ClientResponse response = builder.get(ClientResponse.class);

        Object context = LDHelper.getUserContext();
        
            ResponseBuilder rb;   
            Object data;
            
                try {
                    data = JsonUtils.fromInputStream(response.getEntityInputStream());

                    JsonLdOptions options = new JsonLdOptions();
                    
                    Object framed = JsonLdProcessor.frame(data, context, options);

                    rb = Response.status(response.getStatus()); 

                    rb.entity(JsonUtils.toString(framed));
                    
                } catch (JsonLdError ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
                     return Response.serverError().entity("{}").build();
                } catch (IOException ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
                    return Response.serverError().entity("{}").build();
                }

            return rb.build();
        
        
        /*
        String queryString = LDHelper.prefix+"SELECT ?id ?name WHERE { GRAPH <urn:csc:users> {  ?id a foaf:Person ; foaf:fullName ?name; foaf:mbox "+(email==null||email.equals("undefined")?"?email":"'"+email+"'")+" }}";
        
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(userSparqlEndpoint(), query);

        try {
            ResultSet results = qexec.execSelect();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(b, results);
            return Response.status(200).entity(b.toString()).build();
        } catch (QueryExceptionHTTP ex) {
          Logger.getLogger(User.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.status(400).build(); 
        } finally {
            qexec.close();
        } 
    
        */
        
    }


    /* This should not be REST method */
    @PUT
    @ApiOperation(value = "Add new user", notes = "PUT user to service")
      @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    public Response addNewUser(@Context HttpServletRequest req, @ApiParam(value = "Fullname", required = true) @QueryParam("fullName") String fullName, @ApiParam(value = "email", required = true) @QueryParam("email") String email) {
        
        HttpSession session = req.getSession(true);
    	Object auth = session.getAttribute("auth");
        if(auth!=null) {
            System.out.println("Authenticated?");
        }
        
        UUID userID = UUID.randomUUID();

        String query = "INSERT { GRAPH <urn:csc:users> { <urn:uuid:"+userID.toString()+"> a foaf:Person ; foaf:fullName ?name ; foaf:mbox ?email }} WHERE {  }";
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setLiteral("name", fullName);
        pss.setLiteral("email", email);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,userSparqlUpdateEndpoint());
        
        try {
            qexec.execute();
        } catch(Exception ex) {
            return Response.status(400).build(); 
        }
        
        
        return Response.status(200).build(); 

    }

}