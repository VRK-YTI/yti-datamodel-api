package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/exportModel")
@Tag(name = "Model")
public class ExportModel {

    private static final Logger logger = LoggerFactory.getLogger(ExportModel.class.getName());

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final ContextWriter contextWriter;
    private final JsonSchemaWriter jsonSchemaWriter;
    private final OpenAPIWriter openAPIWriter;
    private final XMLSchemaWriter xmlSchemaWriter;

    @Autowired
    ExportModel(IDManager idManager,
                JerseyResponseManager jerseyResponseManager,
                JerseyClient jerseyClient,
                ContextWriter contextWriter,
                JsonSchemaWriter jsonSchemaWriter,
                OpenAPIWriter openAPIWriter,
                XMLSchemaWriter xmlSchemaWriter) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.contextWriter = contextWriter;
        this.jsonSchemaWriter = jsonSchemaWriter;
        this.openAPIWriter = openAPIWriter;
        this.xmlSchemaWriter = xmlSchemaWriter;
    }

    @GET
    @Operation(description = "Get model from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid model id"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getExportModel(
        @Parameter(description = "Requested resource", schema = @Schema(defaultValue = "default")) @QueryParam("graph") String graph,
        @Parameter(description = "Raw / PlainText boolean", schema = @Schema(defaultValue = "false")) @QueryParam("raw") boolean raw,
        @Parameter(description = "Languages to export") @QueryParam("lang") String lang,
        @Parameter(description = "Content-type", required = true, schema = @Schema(allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json,application/xml,application/vnd.oai.openapi+json")) @QueryParam("content-type") String ctype) {

        /* Check that URIs are valid */
        if (idManager.isInvalid(graph)) {
            return jerseyResponseManager.invalidIRI();
        } else {
            if(graph.contains("#")) {
                graph = graph.split("\\#")[0];
            }
        }

        if (ctype == null) ctype = "application/ld+json";

        ctype = ctype.replace(" ", "+");

        logger.info("Exporting format: " + ctype);

        if (ctype.equals("application/ld+json+context")) {
            String context = contextWriter.newModelContext(graph);
            if (context != null) {
                return jerseyResponseManager.ok(context, raw ? "text/plain;charset=utf-8" : "application/json");
            } else {
                return jerseyResponseManager.notFound();
            }
        } else if (ctype.equals("application/vnd+oai+openapi+json")) {
            String apiStub = openAPIWriter.newOpenApiStub(graph, lang);
            if (apiStub != null) {
                return jerseyResponseManager.ok(apiStub, raw ? "text/plain;charset=utf-8" : "application/json");
            }
        } else if (ctype.equals("application/schema+json")) {
            String schema = null;
            if (lang != null && !lang.equals("undefined") && !lang.equals("null")) {
                logger.info("Exporting schema in " + lang);
                schema = jsonSchemaWriter.newModelSchema(graph, lang);
            } else {
                schema = jsonSchemaWriter.newMultilingualModelSchema(graph);
            }
            if (schema != null) {
                return jerseyResponseManager.ok(schema, raw ? "text/plain;charset=utf-8" : "application/schema+json");
            } else {
                return jerseyResponseManager.langNotDefined();
            }
        } else if (ctype.equals("application/xml")) {

            String schema = xmlSchemaWriter.newModelSchema(graph, lang);

            if (schema != null) {
                return jerseyResponseManager.ok(schema, raw ? "text/plain;charset=utf-8" : "application/xml");
            } else {
                return jerseyResponseManager.langNotDefined();
            }
        }

        /* IF ctype is none of the above try to export graph in RDF format */
        return jerseyClient.getExportGraph(graph, raw, lang, ctype);
    }
}
