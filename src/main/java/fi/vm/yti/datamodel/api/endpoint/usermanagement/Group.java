/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.JerseyClient;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("groups")
@Api(tags = {"Deprecated"}, description = "Edit groups")
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
		return JerseyClient.getGraphResponseFromService("urn:csc:groups",services.getCoreReadAddress());
	}

}
