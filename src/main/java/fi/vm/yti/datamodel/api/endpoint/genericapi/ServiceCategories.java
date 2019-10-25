package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
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
@Path("v1/serviceCategories")
@Tag(name = "Model")
public class ServiceCategories {

    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;

    @Autowired
    ServiceCategories(EndpointServices endpointServices,
                      JerseyClient jerseyClient) {

        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces({ "application/json", "application/ld+json" })
    @Operation(description = "Returns list of service categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "")
    })
    public Response getServiceCategories(@HeaderParam("Accept") String header) {
        if (header != null && header.equals("application/ld+json")) {
            return jerseyClient.constructGraphFromService(QueryLibrary.constructServiceCategories, endpointServices.getCoreSparqlAddress());
        } else {
            return Response.status(200).entity(ServiceCategory.values()).build();
        }
    }
}
