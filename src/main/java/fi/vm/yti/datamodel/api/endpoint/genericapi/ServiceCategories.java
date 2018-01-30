package fi.vm.yti.datamodel.api.endpoint.genericapi;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("serviceCategories")
@Api(tags = {"Model"}, description = "Get available service categories")
public class ServiceCategories {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ServiceCategories.class.getName());

    @GET
    @Produces({"application/json","application/ld+json"})
    @ApiOperation(value = "Returns list of service categories", notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "")
    })
    public Response json(@HeaderParam("Accept") String header) {
        if(header!=null && header.equals("application/ld+json")) {
            return JerseyClient.constructGraphFromService(QueryLibrary.constructServiceCategories, services.getCoreSparqlAddress());
        }
        else {
            return Response.status(200).entity(ServiceCategory.values()).build();
        }
    }

}
