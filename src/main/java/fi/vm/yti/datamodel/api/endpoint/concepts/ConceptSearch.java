/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.TerminologyManager;
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
@Path("v1/conceptSearch")
@Tag(name = "Concept")
public class ConceptSearch {

    private final TerminologyManager terminologyManager;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    ConceptSearch(JerseyResponseManager jerseyResponseManager,
                  TerminologyManager terminologyManager) {

        this.terminologyManager = terminologyManager;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation( description = "Get available concepts from Concept API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Concepts"),
        @ApiResponse(responseCode = "406", description = "Term not defined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response searchConcept(
        @Parameter(description = "Search term", required = true)
        @QueryParam("term") String term,
        @Parameter(description = "Terminology URI")
        @QueryParam("terminologyUri") String terminologyUri) {

        if(term==null) {
            return jerseyResponseManager.invalidParameter();
        }

        return terminologyManager.searchConceptFromTerminologyIntegrationAPI(term, terminologyUri, null);

    }
}
