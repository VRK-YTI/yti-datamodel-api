/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.TermedTerminologyManager;
import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/conceptSearch")
@Api(tags = { "Concept" }, description = "Concepts search from termed")
public class ConceptSearch {

    private final TermedTerminologyManager termedTerminologyManager;
    private final JerseyResponseManager jerseyResponseManager;
    private static final Logger logger = LoggerFactory.getLogger(ConceptSearch.class.getName());

    @Autowired
    ConceptSearch(JerseyResponseManager jerseyResponseManager,
                  TermedTerminologyManager termedTerminologyManager) {

        this.termedTerminologyManager = termedTerminologyManager;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get available concepts", notes = "Search from termed API")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Concepts"),
        @ApiResponse(code = 406, message = "Term not defined"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response concept(
        @ApiParam(value = "Search term", required = true)
        @QueryParam("term") String term,
        @ApiParam(value = "Terminology URI")
        @QueryParam("terminologyUri") String terminologyUri) {

        if(term==null) {
            return jerseyResponseManager.invalidParameter();
        }

        return termedTerminologyManager.searchConceptFromTerminologyIntegrationAPI(term, terminologyUri);

    }
}
