package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("serviceDescription")
@Api(tags = {"Model"}, description = "IOW service description")
public class ServiceDescription {

    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;

    @Autowired
    ServiceDescription(JerseyResponseManager jerseyResponseManager,
                       JerseyClient jerseyClient,
                       EndpointServices endpointServices) {
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
    }

    @GET
    @Produces({"application/ld+json","application/rdf+xml","text/turtle"})
    @ApiOperation(value = "Get service description", notes = "More notes about this method")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid model supplied"),
            @ApiResponse(code = 404, message = "Service description not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(@HeaderParam("Accept") String header) {
        if(header==null || header.equals(("undefined")))
            return jerseyResponseManager.invalidParameter();
        else
            return jerseyClient.getGraphResponseFromService("urn:csc:iow:sd", endpointServices.getCoreReadAddress(), header, false);
    }
}
