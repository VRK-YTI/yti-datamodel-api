/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.usermanagement;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("user")
@Api(value = "/user", description = "Get user")
public class User {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();

    @GET
    @ApiOperation(value = "Get user id", notes = "Get user from service")
      @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Logged in"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 401, message = "Not logged in"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    @Produces("application/ld+json")
    public Response getUser(@Context HttpServletRequest request) {
        
        HttpSession session = request.getSession(); 
        LoginSession login = new LoginSession(session);
 
        if(login.isLoggedIn()){
            Logger.getLogger(User.class.getName()).log(Level.INFO, "User is logged in with: "+login.getEmail());
        } else {
             return Response.status(401).entity("{\"errorMessage\":\"Unauthorized\"}").build();
        }
        
        String email = login.getEmail();
        
        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
        queryString = "CONSTRUCT { "
                + "?id a foaf:Person ; foaf:name ?name . "
                + "?id iow:login ?login . "
                + "?id dcterms:created ?created . "
                + "?id dcterms:modified ?modified . "
                + "?id dcterms:isPartOf ?group . "
                + "?id dcterms:isAdminOf ?group . "
                + "} WHERE { "
                + "GRAPH <urn:csc:users> { "
                + "?id a foaf:Person ; foaf:name ?name; foaf:mbox ?email ; "
                + "dcterms:created ?created . "
                + "OPTIONAL { ?id dcterms:isPartOf ?group . } "
                + "OPTIONAL { ?id iow:isAdminOf ?group . } "
                + "OPTIONAL { ?id dcterms:modified ?modified . }"
                + "}}"; 
         
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setLiteral("email", email);
        pss.setLiteral("login", login.isLoggedIn());

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getUsersSparqlAddress());

    }
    
}