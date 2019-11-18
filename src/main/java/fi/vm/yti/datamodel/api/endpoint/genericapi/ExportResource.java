package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/exportResource")
@Tag(name = "Resource")
public class ExportResource {

    private static final Logger logger = LoggerFactory.getLogger(ExportResource.class.getName());

    private final EndpointServices endpointServices;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final ContextWriter contextWriter;
    private final JsonSchemaWriter jsonSchemaWriter;
    private final XMLSchemaWriter xmlSchemaWriter;
    private final JerseyClient jerseyClient;
    private final OpenAPIWriter openAPIWriter;

    @Autowired
    ExportResource(EndpointServices endpointServices,
                   IDManager idManager,
                   JerseyResponseManager jerseyResponseManager,
                   ContextWriter contextWriter,
                   JsonSchemaWriter jsonSchemaWriter,
                   XMLSchemaWriter xmlSchemaWriter,
                   OpenAPIWriter openAPIWriter,
                   JerseyClient jerseyClient) {
        this.endpointServices = endpointServices;
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.contextWriter = contextWriter;
        this.jsonSchemaWriter = jsonSchemaWriter;
        this.xmlSchemaWriter = xmlSchemaWriter;
        this.openAPIWriter = openAPIWriter;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Operation(description = "Export singe resource")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid model id"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response exportResource(
        @HeaderParam("Accept") String accept,
        @Parameter(description = "Requested resource", schema = @Schema(defaultValue = "default")) @QueryParam("graph") String graph,
        @Parameter(description = "Raw / PlainText boolean", schema = @Schema(defaultValue = "false")) @QueryParam("raw") boolean raw,
        @Parameter(description = "Languages to export") @QueryParam("lang") String lang,
        @Parameter(description = "Service to export") @QueryParam("service") String serviceString,
        @Parameter(description = "Content-type", schema = @Schema(allowableValues = {"application/ld+json","text/turtle","application/rdf+xml","application/ld+json+context","application/schema+json","application/xml"})) @QueryParam("content-type") String ctype) {

        if (ctype == null || ctype.equals("undefined")) ctype = accept;

        String service = endpointServices.getCoreReadAddress();

        if (serviceString != null && !serviceString.equals("undefined")) {
            if (serviceString.equals("concept")) {
                service = endpointServices.getTempConceptReadWriteAddress();
            }
        }

        /* Check that URIs are valid */
        if (idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        }

        if (ctype.equals("application/ld+json+context")) {
            String context = contextWriter.newResourceContext(graph);
            if (context != null) {
                return jerseyResponseManager.ok(context, raw ? "text/plain;charset=utf-8" : "application/json");
            } else {
                return jerseyResponseManager.notFound();
            }
        } else if (ctype.equals("application/vnd+oai+openapi+json")) {
            String apiStub = openAPIWriter.newOpenApiStubFromClass(graph, lang);
            if (apiStub != null) {
                return jerseyResponseManager.ok(apiStub, raw ? "text/plain;charset=utf-8" : "application/json");
            }
        } else if (ctype.equals("application/schema+json")) {
            String schema = jsonSchemaWriter.newResourceSchema(graph, lang);
            if (schema != null) {
                return jerseyResponseManager.ok(schema, raw ? "text/plain;charset=utf-8" : "application/schema+json");
            } else {
                return jerseyResponseManager.notFound();
            }
        } else if (ctype.equals("application/xml")) {

            String schema = xmlSchemaWriter.newClassSchema(graph, lang);

            if (schema != null) {
                return jerseyResponseManager.ok(schema, raw ? "text/plain;charset=utf-8" : "application/schema+json");
            } else {
                return jerseyResponseManager.notFound();
            }
        }

        try {
            ContentType contentType = ContentType.create(ctype);
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);

            if (rdfLang == null) {
                // Default to turtle
                rdfLang = Lang.TURTLE;
            }

            return jerseyClient.getGraphResponseFromService(graph, service, contentType.getContentType(), raw);
        } catch (Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return jerseyResponseManager.serverError();
        }

    }
}
