package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Path("v1/resolveNamespace")
@Api(tags = { "Admin" }, description = "Import for external references")
public class ResolveNamespace {

    private final EndpointServices endpointServices;
    private final NamespaceManager namespaceManager;
    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    ResolveNamespace(EndpointServices endpointServices,
                     NamespaceManager namespaceManager,
                     JerseyClient jerseyClient,
                     JerseyResponseManager jerseyResponseManager) {
        this.endpointServices = endpointServices;
        this.namespaceManager = namespaceManager;
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get service description", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service description not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response getJson(@ApiParam(value = "Namespace", required = true) @QueryParam("namespace") String namespace) {

        if (namespaceManager.resolveNamespace(namespace, null, false)) {
            return jerseyClient.getGraphResponseFromService(namespace, endpointServices.getImportsReadAddress());
        } else {
            return jerseyResponseManager.invalidIRI();
        }

    }

    @POST
    @Produces("application/json")
    @ApiOperation(value = "Get service description", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service description not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(@ApiParam(value = "Namespace", required = true) @QueryParam("namespace") String namespace,
                         @ApiParam(value = "Alternative url for the RDF") @QueryParam("altURL") String alternativeURL,
                         @ApiParam(value = "Force update", required = true) @QueryParam("force") boolean force) {

        if (namespaceManager.resolveNamespace(namespace, (alternativeURL != null && !alternativeURL.equals("undefined") ? alternativeURL : null), force)) {
            return jerseyResponseManager.okEmptyContent();
        } else {
            return jerseyResponseManager.invalidIRI();
        }
    }
}
