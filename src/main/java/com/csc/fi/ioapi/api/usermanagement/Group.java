/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.usermanagement;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.POST;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("groups")
@Api(value = "/groups", description = "Edit groups")
public class Group {

	@Context ServletContext context;
        EndpointServices services = new EndpointServices();
        private static final Logger logger = Logger.getLogger(Group.class.getName());

	@GET
	@ApiOperation(value = "Get groups", notes = "")
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Graph is saved"),
			@ApiResponse(code = 400, message = "Invalid graph supplied"),
			@ApiResponse(code = 404, message = "Service not found") })
	@Produces("application/ld+json")
	public Response getGroup() {
                /* TODO: Join group graphs on allow editing */
		return JerseyFusekiClient.getGraphResponseFromService("urn:csc:groups",services.getCoreReadAddress());
	}

        /*

	@PUT
	@ApiOperation(value = "Add new group", notes = "PUT Body should be json-ld")
	@ApiResponses(value = { @ApiResponse(code = 204, message = "New group is created"),
			@ApiResponse(code = 400, message = "Invalid graph supplied"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response addNewGroup(
           @ApiParam(value = "Group", required = true) String groupID,
           @ApiParam(value = "New group in application/ld+json", required = true) String body,
           @Context HttpServletRequest request) {
           
            IRI groupIRI;
       
            try {
                IRIFactory iri = IRIFactory.semanticWebImplementation();
                groupIRI = iri.construct(groupID);
            } catch (IRIException e) {
                logger.log(Level.WARNING, "GROUP ID is invalid IRI!");
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            } 
            
            if(GraphManager.isExistingGraph(groupIRI)) {
                return Response.status(401).entity(ErrorMessage.USEDIRI).build();
            }
            
            HttpSession session = request.getSession();

            if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

            LoginSession login = new LoginSession(session);

            if(!login.isLoggedIn() || !login.isAdminOfGroup(groupID)) {
                return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
            }

		try {

                    ClientResponse response = JerseyFusekiClient.putGraphToTheService(groupID, body, services.getCoreReadWriteAddress());

                    if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                        Logger.getLogger(Group.class.getName()).log(Level.WARNING,
                                        "Group was not updated! Status " + response.getStatus());
                        return Response.status(response.getStatus()).build();
                    }

                    logger.log(Level.INFO, "Group added sucessfully!");

                    return Response.status(204).build();

		} catch (UniformInterfaceException | ClientHandlerException ex) {
			Logger.getLogger(Group.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
		}


	}
        
        @POST
        @ApiOperation(value = "Add new group", notes = "PUT Body should be json-ld")
        @ApiResponses(value = { @ApiResponse(code = 204, message = "New group is created"),
                        @ApiResponse(code = 400, message = "Invalid graph supplied"),
                        @ApiResponse(code = 404, message = "Service not found") })
        public Response updateGroup(
           @ApiParam(value = "Group", required = true) String groupID,
           @ApiParam(value = "New group metadata in application/ld+json", required = true) String body,
           @Context HttpServletRequest request) {

            IRI groupIRI;

            try {
                IRIFactory iri = IRIFactory.semanticWebImplementation();
                groupIRI = iri.construct(groupID);
            } catch (IRIException e) {
                logger.log(Level.WARNING, "GROUP ID is invalid IRI!");
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            } 

            if(!GraphManager.isExistingGraph(groupIRI)) {
                return Response.status(401).entity(ErrorMessage.NOTFOUND).build();
            }

            HttpSession session = request.getSession();

            if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

            LoginSession login = new LoginSession(session);

            if(!login.isLoggedIn() || !login.isAdminOfGroup(groupID)) {
                return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
            }

            try {

                    ClientResponse response = JerseyFusekiClient.putGraphToTheService(groupID, body, services.getCoreReadWriteAddress());

                    if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                        Logger.getLogger(Group.class.getName()).log(Level.WARNING,
                                        "Group was not updated! Status " + response.getStatus());
                        return Response.status(response.getStatus()).build();
                    }

                    logger.log(Level.INFO, "Group added sucessfully!");

                    return Response.status(204).build();

                } catch (UniformInterfaceException | ClientHandlerException ex) {
                        Logger.getLogger(Group.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
                        return Response.status(400).build();
                }


        }
        
        */
}
