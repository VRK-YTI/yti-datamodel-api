/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@Component
@Path("v1/codeServer")
@Api(tags = { "Codes" }, description = "Available code servers")
public class CodeServer {

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get available code servers", notes = "Get list of available code servers")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Concepts"),
        @ApiResponse(code = 406, message = "Term not defined"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response codeServer() {

        ResponseBuilder rb;

        rb = Response.status(Status.OK);
        rb.type("application/ld+json");
        rb.entity(LDHelper.getDefaultCodeServers());

        return rb.build();
    }
}
