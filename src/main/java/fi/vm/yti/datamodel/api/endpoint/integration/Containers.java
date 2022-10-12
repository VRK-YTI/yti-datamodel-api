package fi.vm.yti.datamodel.api.endpoint.integration;

import java.util.Date;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.IntegrationAPIResponse;
import fi.vm.yti.datamodel.api.index.model.IntegrationContainerRequest;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.joda.time.DateTime;
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
@Tag(name = "Integration")
public class Containers {

    @Context
    UriInfo uriInfo;

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
    @Operation(description = "Get containers from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response listContainers(
        @Parameter(description = "Uris") @QueryParam("uri") String uri,
        @Parameter(description = "Language") @QueryParam("language") String lang,
        @Parameter(description = "Status") @QueryParam("status") String status,
        @Parameter(description = "Type values: library or profile") @QueryParam("type") String type,
        @Parameter(description = "After") @QueryParam("after") String after,
        @Parameter(description = "Before") @QueryParam("before") String before,
        @Parameter(description = "Search") @QueryParam("searchTerm") String search,
        @Parameter(description = "Pagesize") @QueryParam("pageSize") Integer pageSize,
        @Parameter(description = "From") @QueryParam("from") Integer from,
        @Parameter(description = "Include incomplete") @QueryParam("includeIncomplete") boolean includeIncomplete,
        @Parameter(description = "Include incomplete from organization") @QueryParam("includeIncompleteFrom") String includeIncompleteFrom) {

        String path = uriInfo.getAbsolutePath().toString();

        Date afterDate = null;
        if(after!=null && !after.isEmpty()) {
            afterDate = (new DateTime(after)).toDate();
        }

        Date beforeDate = null;
        if(before!=null && !before.isEmpty()) {
            beforeDate = (new DateTime(before)).toDate();
        }

        IntegrationContainerRequest req = new IntegrationContainerRequest(searchIndexManager.parseStringList(uri), search, lang, searchIndexManager.parseStringList(status), type, afterDate, beforeDate, null, pageSize, from, includeIncomplete, searchIndexManager.parseStringList(includeIncompleteFrom));
        IntegrationAPIResponse resp = searchIndexManager.searchContainers(req,path);
        return jerseyResponseManager.ok(objectMapper.valueToTree(resp));

    }

    @POST
    @Operation(description = "Search containers from service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchContainers(@RequestBody IntegrationContainerRequest request) {
        IntegrationAPIResponse response = searchIndexManager.searchContainers(request,null);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

}
