/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.service.TermedTerminologyManager;
import io.swagger.annotations.*;

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

    @Autowired
    ConceptSearch(TermedTerminologyManager termedTerminologyManager) {
        this.termedTerminologyManager = termedTerminologyManager;
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
        @ApiParam(value = "Term", required = true)
        @QueryParam("term") String term,
        @ApiParam(value = "terminology")
        @QueryParam("terminology") String terminology) {

        if (!term.endsWith("*")) {
            term += "*";
        }

        return termedTerminologyManager.searchConceptFromTerminologyIntegrationAPI(term, terminology);

    }
}
