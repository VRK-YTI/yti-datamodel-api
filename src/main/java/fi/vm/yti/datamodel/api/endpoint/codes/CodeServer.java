/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@Component
@Path("v1/codeServer")
@Tag(name = "Codes")
public class CodeServer {

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get available code servers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Concepts"),
        @ApiResponse(responseCode = "406", description = "Term not defined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response codeServer() {

        ResponseBuilder rb;

        rb = Response.status(Status.OK);
        rb.type("application/ld+json");
        rb.entity(LDHelper.getDefaultCodeServers());

        return rb.build();
    }
}
