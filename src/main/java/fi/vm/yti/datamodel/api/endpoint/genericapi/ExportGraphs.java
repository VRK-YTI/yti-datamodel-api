package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.*;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("exportGraphs")
@Api(tags = {"Admin"}, description = "Export graphs")
public class ExportGraphs {

    @Context
    ServletContext context;
    EndpointServices services = new EndpointServices();

    private static final Logger logger = Logger.getLogger(ExportGraphs.class.getName());

    @GET
    @ApiOperation(value = "Get graphs from service", notes = "Exports whole service")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @ApiParam(value = "Requested resource", required = true, allowableValues = "core,prov") @QueryParam("service") String service,
            @ApiParam(value = "Content-type", required = true, allowableValues = "application/ld+json,text/triq") @QueryParam("content-type") String ctype) {

            Lang clang = RDFLanguages.contentTypeToLang(ctype);

            if(clang==null) {
                return JerseyResponseManager.invalidParameter();
            }

            ctype = ctype.replace(" ", "+");

            return JerseyJsonLDClient.getGraphsAsResponse(service, ctype);

    }

}
