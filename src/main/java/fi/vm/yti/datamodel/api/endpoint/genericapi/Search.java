package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.SearchManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/search")
@Tag(name = "Resource")
public class Search {

    private final JerseyResponseManager jerseyResponseManager;
    private final IDManager idManager;
    private final SearchManager searchManager;

    @Autowired
    Search(JerseyResponseManager jerseyResponseManager,
           IDManager idManager,
           SearchManager searchManager) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.idManager = idManager;
        this.searchManager = searchManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Search resources")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Query parse error"),
        @ApiResponse(responseCode = "500", description = "Query exception"),
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response searchResources(
        @Parameter(description = "Search in graph") @QueryParam("graph") String graph,
        @Parameter(description = "Searchstring", required = true) @QueryParam("search") String search,
        @Parameter(description = "Language") @QueryParam("lang") String lang) {

        if (graph == null || graph.equals("undefined") || graph.equals("default")) {
            return jerseyResponseManager.okModel(searchManager.search(null, search, lang));
        } else {
            if (!idManager.isValidUrl(graph)) {
                return jerseyResponseManager.invalidParameter();
            } else {
                return jerseyResponseManager.okModel(searchManager.search(graph, search, lang));
            }
        }
    }
}
