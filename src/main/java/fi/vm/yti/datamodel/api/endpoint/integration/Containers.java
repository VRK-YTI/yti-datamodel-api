package fi.vm.yti.datamodel.api.endpoint.integration;

import java.util.Date;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.IntegrationAPIResponse;
import fi.vm.yti.datamodel.api.index.model.IntegrationContainerRequest;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;



@Component
@Path("v1/integration/containers")
@Api(tags = { "Integration" }, description = "Operations about models")
public class Containers {

    @Context
    UriInfo uriInfo;

    private static final Logger logger = LoggerFactory.getLogger(Containers.class.getName());
    private final SearchIndexManager searchIndexManager;
    private final JerseyResponseManager jerseyResponseManager;
    private ObjectMapper objectMapper;

    @Autowired
    Containers(
        SearchIndexManager searchIndexManager,
        JerseyResponseManager jerseyResponseManager,
        ObjectMapper objectMapper) {
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Get containers from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response listContainers(
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
        @QueryParam("from") Integer from,
        @ApiParam(value = "IncludeIncomplete")
        @QueryParam("includeIncomplete") boolean includeIncomplete,
        @ApiParam(value = "IncludeIncomplete")
        @QueryParam("includeIncomplete") String includeIncompleteFrom) {

        String path = uriInfo.getAbsolutePath().toString();
        IntegrationContainerRequest req = new IntegrationContainerRequest(search, lang, searchIndexManager.parseStringList(status), after, null, pageSize, from, includeIncomplete, searchIndexManager.parseStringList(includeIncompleteFrom));
        IntegrationAPIResponse resp = searchIndexManager.searchContainers(req,path);
        return jerseyResponseManager.ok(objectMapper.valueToTree(resp));

    }

    @POST
    @ApiOperation(value = "Search containers from service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchContainers(@RequestBody IntegrationContainerRequest request) {
        IntegrationAPIResponse response = searchIndexManager.searchContainers(request,null);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

}
