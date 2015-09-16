/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.usermanagement;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.LDHelper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

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
       return Endpoint.getEndpoint() + "/users/data";
    }
    
    public String userSparqlEndpoint() {
       return Endpoint.getEndpoint() + "/users/sparql";
    }
    
    public String userSparqlUpdateEndpoint() {
       return Endpoint.getEndpoint() + "/users/update";
    }
     
    @GET
    @ApiOperation(value = "Get user id", notes = "Get user from service")
      @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    @Produces("application/ld+json")
    public Response getUser(@ApiParam(value = "email") @QueryParam("email") String email, @Context HttpServletRequest request) {
       
        HttpSession session = request.getSession();
        
        LoginSession login = new LoginSession(session);
        
        if(session.getAttribute("mail")!=null){
            Logger.getLogger(User.class.getName()).log(Level.INFO, "USER mail: "+session.getAttribute("mail"));
        } else {
             Logger.getLogger(User.class.getName()).log(Level.INFO, "Not logged in?");
        }
        
        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
        ResponseBuilder rb;

        queryString = "CONSTRUCT { ?id a foaf:Person ; foaf:name ?name . ?id iow:login ?login . ?id dcterms:isPartOf ?group . } WHERE { GRAPH <urn:csc:users> { ?id a foaf:Person ; foaf:name ?name; foaf:mbox ?email . OPTIONAL { ?id dcterms:isPartOf ?group .}}}"; 
         
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
          
        // If looking with certain email
        if(email!=null && !email.equals("undefined")) {
              pss.setLiteral("email", email);
              
              if(email.equals("testi@example.org"))
                    pss.setLiteral("login", true);
                  else
                    pss.setLiteral("login", login.isLoggedIn());
              
        } 
        

        Client client = Client.create();
         
        WebResource webResource = client.resource(userSparqlEndpoint())
                                  .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

        WebResource.Builder builder = webResource.accept("application/ld+json");

        ClientResponse response = builder.get(ClientResponse.class);

        
        rb = Response.status(response.getStatus()); 
        rb.entity(response.getEntityInputStream());
       
     
            /*
             try {
                    Object context = LDHelper.getUserContext();
                    Object data = JsonUtils.fromInputStream(response.getEntityInputStream());
                    JsonLdOptions options = new JsonLdOptions();
                    Object framed = JsonLdProcessor.frame(data, context, options);
                    rb = Response.status(response.getStatus()); 
                    rb.entity(JsonUtils.toString(framed));
                } catch (JsonLdError ex) {
                    Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
                     return Response.serverError().entity("{}").build();
                } catch (IOException ex) {
                    Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
                    return Response.serverError().entity("{}").build();
                }
*/
        
            return rb.build();
        
    }


    /* TODO: This for testing only (SHOULD BE REMOVED) */
   
    @PUT
    @ApiOperation(value = "Add new user", notes = "PUT user to service")
      @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    public Response addNewUser(@ApiParam(value = "New users in application/ld+json", required = true) String body) {
  
        	try {

			String service = userEndpoint();
			Client client = Client.create();
			WebResource webResource = client.resource(service).queryParam("graph", "urn:csc:users");

			WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
			ClientResponse response = builder.put(ClientResponse.class, body);

			if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
				Logger.getLogger(Group.class.getName()).log(Level.WARNING,
						"Group was not updated! Status " + response.getStatus());
				return Response.status(response.getStatus()).build();
			}

			Logger.getLogger(Group.class.getName()).log(Level.INFO, "Group added sucessfully!");
			return Response.status(204).build();

		} catch (UniformInterfaceException | ClientHandlerException ex) {
			Logger.getLogger(Group.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
		}
    
        
        /*
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
*/
    }
}