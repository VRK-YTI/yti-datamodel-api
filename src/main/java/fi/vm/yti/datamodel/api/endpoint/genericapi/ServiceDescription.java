package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/serviceDescription")
@Tag(name = "Model")
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
    @Produces({ "application/ld+json", "application/rdf+xml", "text/turtle" })
    @Operation(description = "Get service description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service description not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getServiceDescription(@HeaderParam("Accept") String header) {
        if (header == null || header.equals(("undefined")))
            return jerseyResponseManager.invalidParameter();
        else
            return jerseyClient.getGraphResponseFromService("urn:csc:iow:sd", endpointServices.getCoreReadAddress(), header, false);
    }
}
