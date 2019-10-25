package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/exportGraphs")
@Tag(name = "Admin")
public class ExportGraphs {

    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    ExportGraphs(JerseyClient jerseyClient,
                 JerseyResponseManager jerseyResponseManager) {
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces({ "application/ld+json" })
    @Operation(description = "Get graphs from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getExportGraphs(
        @Parameter(description = "Requested resource", required = true) @QueryParam("service") String service,
        @Parameter(description = "Content-type", required = true) @QueryParam("content-type") String ctype) {

        // TODO: allowableValues = "core,prov"
        // TODO: allowableValues = "application/ld+json"

        Lang clang = RDFLanguages.contentTypeToLang(ctype);

        if (clang == null) {
            return jerseyResponseManager.invalidParameter();
        }

        ctype = ctype.replace(" ", "+");

        return jerseyClient.getGraphsAsResponse(service, ctype);
    }
}
