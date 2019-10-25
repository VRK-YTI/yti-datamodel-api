package fi.vm.yti.datamodel.api.endpoint.integration;

import java.util.Date;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.IntegrationAPIResponse;
import fi.vm.yti.datamodel.api.index.model.IntegrationResourceRequest;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Path("v1/integration/resources")
@Tag(name = "Integration")
public class Resources {

    private static final Logger logger = LoggerFactory.getLogger(Resources.class.getName());
    private final SearchIndexManager searchIndexManager;
    private final JerseyResponseManager jerseyResponseManager;
    private ObjectMapper objectMapper;

    @Context
    UriInfo uriInfo;

    @Autowired
    Resources(
        SearchIndexManager searchIndexManager,
        JerseyResponseManager jerseyResponseManager,
        ObjectMapper objectMapper) {
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces("application/json")
    @Operation(description = "Get resources from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getResources(
        @Parameter(description = "Container") @QueryParam("container") String container,
        @Parameter(description = "Language") @QueryParam("language") String lang,
        @Parameter(description = "Status") @QueryParam("status") String status,
        @Parameter(description = "Type values: class, shape, attribute, association") @QueryParam("type") String type,
        @Parameter(description = "After") @QueryParam("after") Date after,
        @Parameter(description = "Search") @QueryParam("searchTerm") String search,
        @Parameter(description = "Pagesize") @QueryParam("pageSize") Integer pageSize,
        @Parameter(description = "From") @QueryParam("from") Integer from) {

        String path = uriInfo.getAbsolutePath().toString();
        IntegrationResourceRequest req = new IntegrationResourceRequest(search,lang,container,searchIndexManager.parseStringList(status),type,after,null,pageSize,from);
        IntegrationAPIResponse apiResp = searchIndexManager.searchResources(req,path);
        return jerseyResponseManager.ok(objectMapper.valueToTree(apiResp));

    }

    @POST
    @Operation(description = "Search resources from service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchResources(
        @RequestBody IntegrationResourceRequest request) {
        IntegrationAPIResponse response = searchIndexManager.searchResources(request,null);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }
}
