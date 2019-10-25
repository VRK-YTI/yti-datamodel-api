package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Component
@Path("v1/config")
@Tag(name = "Admin")
public class Config {

    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Config(JerseyResponseManager jerseyResponseManager) {
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @Context
    ServletContext context;

    @GET
    @Operation(description = "Returns API config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response getConfig() {

        return jerseyResponseManager.config();
    }
}
