package fi.vm.yti.datamodel.api.endpoint.genericapi;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.QueryParam;

@Path("serviceCategories")
@Api(tags = {"Model"}, description = "Get available service categories")
public class ServiceCategories {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ServiceCategories.class.getName());

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Returns list of service categories", notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "")
    })
    public Response json() {
        return Response.status(200).entity(ServiceCategory.values()).build();
    }

}
