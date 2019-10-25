package fi.vm.yti.datamodel.api.endpoint.search;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchResponse;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("v1/searchResources")
@Tag(name = "Index")
public class ResourceSearch {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSearch.class.getName());
    private SearchIndexManager searchIndexManager;
    private JerseyResponseManager jerseyResponseManager;
    private ObjectMapper objectMapper;

    @Autowired
    public ResourceSearch(SearchIndexManager searchIndexManager,
                          JerseyResponseManager jerseyResponseManager,
                          ObjectMapper objectMapper) {
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid request!")
    })
    public Response searchModels(ResourceSearchRequest request) {
        ResourceSearchResponse response = searchIndexManager.searchResources(request);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

}
