package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.SearchManager;
import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("search")
@Api(tags = { "Resource" }, description = "Search resources")
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
    @ApiOperation(value = "Sparql query to given service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Query parse error"),
        @ApiResponse(code = 500, message = "Query exception"),
        @ApiResponse(code = 200, message = "OK")
    })
    public Response search(
        @ApiParam(value = "Search in graph") @QueryParam("graph") String graph,
        @ApiParam(value = "Searchstring", required = true) @QueryParam("search") String search,
        @ApiParam(value = "Language") @QueryParam("lang") String lang) {

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
