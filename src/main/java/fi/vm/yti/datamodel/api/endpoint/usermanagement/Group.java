/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("groups")
@Api(tags = {"Deprecated"}, description = "Edit groups")
public class Group {

	private final JerseyClient jerseyClient;
	private final EndpointServices endpointServices;

	@Autowired
	Group(JerseyClient jerseyClient,
		  EndpointServices endpointServices) {
		this.jerseyClient = jerseyClient;
		this.endpointServices = endpointServices;
	}

	@GET
	@ApiOperation(value = "Get groups", notes = "")
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Graph is saved"),
			@ApiResponse(code = 400, message = "Invalid graph supplied"),
			@ApiResponse(code = 404, message = "Service not found") })
	@Produces("application/ld+json")
	public Response getGroup() {
		return jerseyClient.getGraphResponseFromService("urn:csc:groups", endpointServices.getCoreReadAddress());
	}

}
