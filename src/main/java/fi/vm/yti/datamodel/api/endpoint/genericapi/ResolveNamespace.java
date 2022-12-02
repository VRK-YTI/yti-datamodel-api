package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Path("v1/resolveNamespace")
@Tag(name = "Admin")
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
    @Operation(description = "Resolve namespace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service description not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response resolveNamespace(
        @Parameter(description = "Namespace", required = true) @QueryParam("namespace") String namespace,
        @Parameter(description = "Content type for accept header") @QueryParam("accept") String accept) {

        boolean resolved;

        if (accept == null) {
            resolved = namespaceManager.resolveNamespace(namespace, null, false);
        } else {
            resolved = namespaceManager.resolveNamespace(namespace, null, false, accept);
        }

        if (resolved) {
            return jerseyClient.getGraphResponseFromService(namespace, endpointServices.getImportsReadAddress());
        } else {
            return jerseyResponseManager.invalidIRI();
        }

    }

    @POST
    @Produces("application/json")
    @Operation(description = "Resolve namespace from alternative location")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service description not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response resolveNamespaceFromAlternativeLocation(@Parameter(description = "Namespace", required = true) @QueryParam("namespace") String namespace,
                         @Parameter(description = "Alternative url for the RDF") @QueryParam("altURL") String alternativeURL,
                         @Parameter(description = "Force update", required = true) @QueryParam("force") boolean force) {

        if (namespaceManager.resolveNamespace(namespace, (alternativeURL != null && !alternativeURL.equals("undefined") ? alternativeURL : null), force)) {
            return jerseyResponseManager.okEmptyContent();
        } else {
            return jerseyResponseManager.invalidIRI();
        }
    }
}
