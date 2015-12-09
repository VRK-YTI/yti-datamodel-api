/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.usermanagement;

import com.csc.fi.ioapi.config.EndpointServices;
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
		return JerseyFusekiClient.getGraphResponseFromService("urn:csc:groups",services.getCoreReadAddress());
	}

        
        
        /* TODO: Remove or add API key */
	@PUT
	@ApiOperation(value = "Add new group", notes = "PUT Body should be json-ld")
	@ApiResponses(value = { @ApiResponse(code = 204, message = "New group is created"),
			@ApiResponse(code = 400, message = "Invalid graph supplied"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response addNewGroups(@ApiParam(value = "New groups in application/ld+json", required = true) String body) {

		try {

                    ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:csc:groups", body, services.getCoreReadWriteAddress());

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
        
        
        /*
	@POST
	@ApiOperation(value = "Update group", notes = "Add users or change name")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Group is updated"),
			@ApiResponse(code = 400, message = "Invalid graph supplied"),
			@ApiResponse(code = 403, message = "Invalid IRI in parameter"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response modifyGroup(@ApiParam(value = "Group ID", required = true) @QueryParam("group") String group,
			@ApiParam(value = "user") @QueryParam("user") String user,
			@ApiParam(value = "groupname") @QueryParam("groupname") String groupname) {

		IRI groupID;
		try {
			IRIFactory iri = IRIFactory.semanticWebImplementation();
			groupID = iri.construct(group);
		} catch (IRIException e) {
			Logger.getLogger(User.class.getName()).log(Level.WARNING, "GROUP ID " + group + " is invalid IRI!");
			return Response.status(403).build();
		}

		String queryString;
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setNsPrefixes(LDHelper.PREFIX_MAP);

		if (groupname != null && !groupname.equals("undefined")) {
			queryString = "DELETE { GRAPH <urn:csc:groups> { ?group rdfs:label ?newname } } INSERT { GRAPH <urn:csc:groups> { ?group rdfs:label ?group . }} WHERE { GRAPH <urn:csc:groups> {?group a foaf:Group ; rdfs:label ?name . } }";
			pss.setCommandText(queryString);
			pss.setIri("newname", groupname);
		} else if (user != null && !user.equals("undefined")) {

			IRI userID;
			try {
				IRIFactory iri = IRIFactory.semanticWebImplementation();
				userID = iri.construct(group);
			} catch (IRIException e) {
				Logger.getLogger(User.class.getName()).log(Level.WARNING, "USER ID " + user + " is invalid IRI!");
				return Response.status(403).build();
			}

			queryString = "INSERT { GRAPH <urn:csc:groups> { ?group foaf:member ?user . }} WHERE {GRAPH <urn:csc:groups> { ?group a foaf:Group .} }";
			pss.setCommandText(queryString);
			pss.setIri("user", userID);

		} else {
			return Response.status(403).build();
		}

		pss.setIri("group", groupID);

		UpdateRequest query = pss.asUpdate();
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(query, services.getCoreReadWriteAddress());

		try {
			qexec.execute();
			return Response.status(200).build();
		} catch (QueryExceptionHTTP ex) {
			Logger.getLogger(User.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
		}
	}
*/

        
        /*
	@DELETE
	@ApiOperation(value = "Delete group", notes = "PUT Body should be json-ld")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Group is removed"),
			@ApiResponse(code = 403, message = "Invalid ID"), @ApiResponse(code = 404, message = "Service not found") })
	public Response removeGroup(
			@ApiParam(value = "Group ID", required = true) @QueryParam("groupID") String groupIDString) {

		IRI groupID;
		try {
			IRIFactory iri = IRIFactory.semanticWebImplementation();
			groupID = iri.construct(groupIDString);
		} catch (IRIException e) {
			Logger.getLogger(User.class.getName()).log(Level.WARNING, "GROUP ID " + groupIDString + " is invalid IRI!");
			return Response.status(403).build();
		}

		String sparqlQuery = "DELETE{ GRAPH <urn:csc:groups> {?group ?p ?o} } WHERE { GRAPH <urn:csc:groups> {?group ?p ?o }}";
		ParameterizedSparqlString pss = new ParameterizedSparqlString(sparqlQuery);
		pss.setNsPrefixes(LDHelper.PREFIX_MAP);
		pss.setIri("group", groupID);

		UpdateRequest queryObj = pss.asUpdate(); // UpdateFactory.create(parameterizedSparqlString.as);
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getUsersSparqlUpdateAddress());

		try {
			qexec.execute();
			return Response.status(200).build();
		} catch (Exception ex) {
			return Response.status(400).build();
		}

	}
*/
        
}
