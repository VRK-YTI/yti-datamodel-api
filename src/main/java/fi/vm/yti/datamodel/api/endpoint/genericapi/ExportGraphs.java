package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.*;

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
@Path("exportGraphs")
@Api(tags = { "Admin" }, description = "Export graphs")
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
    @ApiOperation(value = "Get graphs from service", notes = "Exports whole service")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "Requested resource", required = true, allowableValues = "core,prov") @QueryParam("service") String service,
        @ApiParam(value = "Content-type", required = true, allowableValues = "application/ld+json") @QueryParam("content-type") String ctype) {

        Lang clang = RDFLanguages.contentTypeToLang(ctype);

        if (clang == null) {
            return jerseyResponseManager.invalidParameter();
        }

        ctype = ctype.replace(" ", "+");

        return jerseyClient.getGraphsAsResponse(service, ctype);
    }
}
