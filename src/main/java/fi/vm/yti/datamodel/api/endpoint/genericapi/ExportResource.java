package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.annotations.*;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Path("exportResource")
@Api(tags = {"Resource"}, description = "Export Classes, Predicates, Shapes etc.")
public class ExportResource {

    private static final Logger logger = Logger.getLogger(ExportResource.class.getName());

    private final EndpointServices endpointServices;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final ContextWriter contextWriter;
    private final JsonSchemaWriter jsonSchemaWriter;
    private final XMLSchemaWriter xmlSchemaWriter;
    private final JerseyClient jerseyClient;

    @Autowired
    ExportResource(EndpointServices endpointServices,
                   IDManager idManager,
                   JerseyResponseManager jerseyResponseManager,
                   ContextWriter contextWriter,
                   JsonSchemaWriter jsonSchemaWriter,
                   XMLSchemaWriter xmlSchemaWriter,
                   JerseyClient jerseyClient) {
        this.endpointServices = endpointServices;
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.contextWriter = contextWriter;
        this.jsonSchemaWriter = jsonSchemaWriter;
        this.xmlSchemaWriter = xmlSchemaWriter;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @ApiOperation(value = "Get model from service", notes = "More notes about this method")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid model supplied"),
            @ApiResponse(code = 403, message = "Invalid model id"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @HeaderParam("Accept") String accept,
            @ApiParam(value = "Requested resource", defaultValue = "default") @QueryParam("graph") String graph,
            @ApiParam(value = "Raw / PlainText boolean", defaultValue = "false") @QueryParam("raw") boolean raw,
            @ApiParam(value = "Languages to export") @QueryParam("lang") String lang,
            @ApiParam(value = "Service to export") @QueryParam("service") String serviceString,
            @ApiParam(value = "Content-type", allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json,application/xml") @QueryParam("content-type") String ctype) {

        if(ctype==null || ctype.equals("undefined")) ctype = accept;

        String service = endpointServices.getCoreReadAddress();

        if(serviceString!=null && !serviceString.equals("undefined")) {
            if(serviceString.equals("concept")) {
                service = endpointServices.getTempConceptReadWriteAddress();
            }
        }

        /* Check that URIs are valid */
        if(idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        }

        if(ctype.equals("application/ld+json+context")) {
            String context = contextWriter.newResourceContext(graph);
            if(context!=null) {
                return jerseyResponseManager.ok(context,raw?"text/plain;charset=utf-8":"application/json");
            } else {
                return jerseyResponseManager.notFound();
            }
        } else if(ctype.equals("application/schema+json")) {
            String schema = jsonSchemaWriter.newResourceSchema(graph,lang);
            if(schema!=null) {
                return jerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/schema+json");
            } else {
                return jerseyResponseManager.notFound();
            }
        } else if(ctype.equals("application/xml")) {

            String schema = xmlSchemaWriter.newClassSchema(graph,lang);

            if(schema!=null) {
                return jerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/schema+json");
            } else {
                return jerseyResponseManager.notFound();
            }
        }


        try {
            ContentType contentType = ContentType.create(ctype);
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);

            if(rdfLang==null) {
                rdfLang = Lang.TURTLE;
                //return JerseyResponseManager.notFound();
            }

            return jerseyClient.getGraphResponseFromService(graph, service, contentType.getContentType(), raw);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return jerseyResponseManager.serverError();
        }

    }
}
