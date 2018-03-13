/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@Component
@Path("codeValues")
@Api(tags = {"Codes"}, description = "Get codevalues with ID")
public class Codes {

    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Codes(JerseyClient jerseyClient,
          EndpointServices endpointServices,
          JerseyResponseManager jerseyResponseManager) {
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get code values with id", notes = "codes will be loaded to TEMP database when used the first time")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "codes"),
            @ApiResponse(code = 406, message = "code not defined"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response concept(
            @ApiParam(value = "uri", required = true)
            @QueryParam("uri") String uri) {

        OPHCodeServer codeServer = new OPHCodeServer(uri, false, endpointServices);

        if(!codeServer.status) {
            boolean codeStatus = codeServer.updateCodes(uri);
            if(!codeStatus) return jerseyResponseManager.notAcceptable();
        }

        return jerseyClient.getGraphResponseFromService(uri, endpointServices.getSchemesReadAddress());
    }

    @PUT
    @ApiOperation(value = "Get code values with id", notes = "Update certain code to temp database")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "codes"),
            @ApiResponse(code = 406, message = "code not defined"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response updateCodes(
            @ApiParam(value = "uri", required = true)
            @QueryParam("uri") String uri) {

        ResponseBuilder rb;

        OPHCodeServer codeServer = new OPHCodeServer(endpointServices);

        boolean status = codeServer.updateCodes(uri);

        rb = Response.status(Status.OK);

        if(status) return rb.entity("{}").build();
        else return jerseyResponseManager.notAcceptable();
    }
}
