/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.TermedTerminologyManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/conceptSchemes")
@Api(tags = { "Concept" }, description = "Available concept schemes from Term editor")
public class ConceptsSchemes {

    private final JerseyResponseManager jerseyResponseManager;
    private final TermedTerminologyManager termedTerminologyManager;

    @Autowired
    ConceptsSchemes(JerseyResponseManager jerseyResponseManager,
                    TermedTerminologyManager termedTerminologyManager) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.termedTerminologyManager = termedTerminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get available concepts", notes = "Lists terminologies from Termeditor")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Concepts"),
        @ApiResponse(code = 406, message = "Term not defined"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response vocab() {
        return jerseyResponseManager.okModel(termedTerminologyManager.getSchemesAsModelFromTermedAPI());
    }
}
