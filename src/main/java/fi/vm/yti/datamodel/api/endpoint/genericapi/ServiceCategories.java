package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
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
@Path("v1/serviceCategories")
@Api(tags = { "Model" }, description = "Get available service categories")
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
    @ApiOperation(value = "Returns list of service categories")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "")
    })
    public Response json(@HeaderParam("Accept") String header) {
        if (header != null && header.equals("application/ld+json")) {
            return jerseyClient.constructGraphFromService(QueryLibrary.constructServiceCategories, endpointServices.getCoreSparqlAddress());
        } else {
            return Response.status(200).entity(ServiceCategory.values()).build();
        }
    }
}
