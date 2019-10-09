/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.service.TerminologyManager;
import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/concept")
@Api(tags = { "Concept" }, description = "Get concept with id")
public class Concept {

    private final TerminologyManager terminologyManager;

    @Autowired
    Concept(TerminologyManager terminologyManager) {
        this.terminologyManager = terminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get available concepts", notes = "Search from finto API & concept temp")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Concepts"),
        @ApiResponse(code = 406, message = "Term not defined"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response concept(
        @ApiParam(value = "uri")
        @QueryParam("uri") String uri) {
        return terminologyManager.searchConceptFromTerminologyIntegrationAPI(null, null, uri);
    }
}
