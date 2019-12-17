package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.update.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

@Component
@Path("v1/sparql")
@Tag(name = "Admin")
public class Sparql {

    private final AuthorizationManager authorizationManager;
    private final EndpointServices endpointServices;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Sparql(AuthorizationManager authorizationManager,
           EndpointServices endpointServices,
           JerseyResponseManager jerseyResponseManager) {
        this.authorizationManager = authorizationManager;
        this.endpointServices = endpointServices;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Consumes("application/sparql-query")
    @Produces("application/sparql-results+json")
    @Operation(description = "Sparql query to given service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Query parse error"),
        @ApiResponse(responseCode = "500", description = "Query exception"),
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response sparql(
        @Parameter(description = "SPARQL Query", required = true) @QueryParam("query") String queryString,
        @Parameter(description = "SPARQL Service", schema = @Schema(defaultValue = "core", allowableValues = {"core","prov","imports","scheme","concept"})) @QueryParam("service") String service,
        @Parameter(description = "Accept", required = true, schema = @Schema(allowableValues = {"application/sparql-results+json","text/csv", "text/turtle"})) @QueryParam("accept") String accept) {

        if (!authorizationManager.hasRightToRunSparqlQuery()) {
            return jerseyResponseManager.unauthorized();
        }

        Query query;

        try {
            queryString = LDHelper.prefix + queryString;
            query = QueryFactory.create(queryString);
        } catch (QueryParseException ex) {
            return Response.status(400).build();
        }

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getSparqlAddress(service), query)) {

            OutputStream outs = new ByteArrayOutputStream();
            ResultSet results = qexec.execSelect();

            if (accept.equals("text/csv")) {
                ResultSetFormatter.outputAsCSV(outs, results);
            } else {
                ResultSetFormatter.outputAsJSON(outs, results);
            }

            return Response
                .ok(outs.toString(), accept)
                .build();

        } catch (QueryException ex) {
            return Response.status(500).build();
        }
    }

    @GET
    @Path("construct")
    @Consumes("application/sparql-query")
    @Produces("text/turtle")
    @Operation(description = "Sparql query to given service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Query parse error"),
        @ApiResponse(responseCode = "500", description = "Query exception"),
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response sparqlConstruct(
        @Parameter(description = "SPARQL Query", required = true) @QueryParam("query") String queryString,
        @Parameter(description = "SPARQL Service", schema = @Schema(defaultValue = "core", allowableValues = {"core","prov","imports","scheme","concept"})) @QueryParam("service") String service,
        @Parameter(description = "Accept", required = true, schema = @Schema(allowableValues = {"text/turtle"})) @QueryParam("accept") String accept) {

        if (!authorizationManager.hasRightToRunSparqlQuery()) {
            return jerseyResponseManager.unauthorized();
        }

        Query query;

        try {
            queryString = LDHelper.prefix + queryString;
            query = QueryFactory.create(queryString);
        } catch (QueryParseException ex) {
            return Response.status(400).build();
        }

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getSparqlAddress(service), query)) {

            OutputStream outs = new ByteArrayOutputStream();
            Model results = qexec.execConstruct();

            StringWriter writer = new StringWriter();
            ContentType contentType = ContentType.create(accept);
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            RDFDataMgr.write(writer, results, RDFWriterRegistry.defaultSerialization(rdfLang));

            return Response
                .ok(writer.toString(), accept)
                .build();

        } catch (QueryException ex) {
            return Response.status(500).build();
        }
    }

    @POST
    @Operation(description = "Sends SPARQL Update query to given service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })

    public Response sparqlUpdate(
        @Parameter(description = "Sparql query", required = true) String body,
        @Parameter(description = "SPARQL Service", schema = @Schema(defaultValue = "core", allowableValues = {"core","prov","imports","scheme","concept"})) @QueryParam("service") String service) {

        if (!authorizationManager.hasRightToRunSparqlQuery()) {
            return jerseyResponseManager.unauthorized();
        }

        String query = LDHelper.prefix + body;

        try {
            UpdateRequest queryObj = UpdateFactory.create(query);
            UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getSparqlUpdateAddress(service));
            qexec.execute();
        } catch (UpdateException | QueryParseException ex) {
            return Response.status(400).build();
        }

        return Response.status(200).build();
    }
}
