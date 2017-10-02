/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

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

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("user")
@Api(tags = {"Users"}, description = "Get user")
public class User {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(User.class.getName());

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
        
        logger.info("Getting user");
            
        HttpSession session = request.getSession(); 
        LoginSession login = new LoginSession(session);
 
        if(login.isLoggedIn()){
            Logger.getLogger(User.class.getName()).log(Level.INFO, "User is logged in with: "+login.getEmail());
        } else {
             return JerseyResponseManager.unauthorized();
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

        return JerseyJsonLDClient.constructGraphFromServiceDirect(pss.toString(), services.getUsersSparqlAddress());

    }
    
}