package fi.vm.yti.datamodel.api.v2.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.v2.elasticsearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.v2.service.FrontendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);
    private final SearchIndexManager searchIndexManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final ObjectMapper objectMapper;
    private final FrontendService frontendService;

    @Autowired
    public FrontendController(SearchIndexManager searchIndexManager,
                       JerseyResponseManager jerseyResponseManager,
                       ObjectMapper objectMapper,
                      FrontendService frontendService) {
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
        this.frontendService = frontendService;
    }


    @GET
    @Operation(summary = "Get counts", description = "List counts of data model grouped by different search results")
    @Path("/counts")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    public Response getCounts() {
        logger.info("GET /counts requested");
        CountSearchResponse response = searchIndexManager.getCounts();
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

    @GET
    @Operation(summary = "Get organizations", description = "List of organizations sorted by name")
    @Path("/organizations")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", description = "Organization list as JSON")
    public Response getOrganizations(
            @QueryParam("sortLang") String sortLang,
            @QueryParam("includeChildOrganizations") boolean includeChildOrganizations) {
        return jerseyResponseManager.ok(
                objectMapper.valueToTree(
                        frontendService.getOrganizations(sortLang, includeChildOrganizations)
                )
        );
    }

    @GET
    @Operation(summary = "Get service categories", description = "List of service categories sorted by name")
    @Path("/serviceCategories")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", description = "Service categories as JSON")
    public Response getServiceCategories(@QueryParam("sortLang") String sortLang) {
        return jerseyResponseManager.ok(
                objectMapper.valueToTree(
                        frontendService.getServiceCategories(sortLang)
                )
        );
    }
}
