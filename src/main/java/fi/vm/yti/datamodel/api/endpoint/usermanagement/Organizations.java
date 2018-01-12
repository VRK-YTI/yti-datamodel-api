/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("organizations")
@Api(tags = {"Organizations"}, description = "Get organizations")
public class Organizations {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Group.class.getName());

    @GET
    @ApiOperation(value = "Get organizations", notes = "Get organizations from rhp")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "List of organizations"),
            @ApiResponse(code = 400, message = "Error from organization service"),
            @ApiResponse(code = 404, message = "Organization service not found") })
    @Produces("application/json")
    public Response getOrganizations() {
        return JerseyResponseManager.okModel(GraphManager.getCoreGraph("urn:yti:organizations"));
    }

}
