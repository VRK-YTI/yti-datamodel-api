package fi.vm.yti.datamodel.api.endpoint.integration;

import java.util.Date;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.IntegrationAPIResponse;
import fi.vm.yti.datamodel.api.index.model.IntegrationResourceRequest;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchResponse;
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import io.swagger.annotations.*;

import org.apache.jena.query.ParameterizedSparqlString;
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
@Api(tags = { "Integration" }, description = "Operations about models")
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
    @ApiOperation(value = "Get resources from service")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response getResources(
        @ApiParam(value = "Container", required = true)
        @QueryParam("container") String container,
        @ApiParam(value = "Language")
        @QueryParam("language") String lang,
        @ApiParam(value = "Status")
        @QueryParam("status") String status,
        @ApiParam(value = "After")
        @QueryParam("after") Date after,
        @ApiParam(value = "Search")
        @QueryParam("searchTerm") String search,
        @ApiParam(value = "Pagesize")
        @QueryParam("pageSize") Integer pageSize,
        @ApiParam(value = "From")
        @QueryParam("from") Integer from) {

        if (container == null) {
            return jerseyResponseManager.invalidParameter();
        }

        String path = uriInfo.getAbsolutePath().toString();
        IntegrationResourceRequest req = new IntegrationResourceRequest(search,lang,container,searchIndexManager.parseStatus(status),after,null,pageSize,from);
        IntegrationAPIResponse apiResp = searchIndexManager.searchResources(req,path);
        return jerseyResponseManager.ok(objectMapper.valueToTree(apiResp));

    }

    @POST
    @ApiOperation(value = "Search resources from service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchResources(
        @RequestBody IntegrationResourceRequest request) {
        IntegrationAPIResponse response = searchIndexManager.searchResources(request,null);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }
}
