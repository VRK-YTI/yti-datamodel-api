package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.*;
import org.apache.jena.query.*;
import org.apache.jena.update.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@Component
@Path("sparql")
@Api(tags = {"Admin"}, description = "Edit resources")
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
    @ApiOperation(value = "Sparql query to given service", notes = "More notes about this method")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Query parse error"),
            @ApiResponse(code = 500, message = "Query exception"),
            @ApiResponse(code = 200, message = "OK")
    })
    public Response sparql(
            @ApiParam(value = "SPARQL Query", required = true) @QueryParam("query") String queryString) {

        if (!authorizationManager.hasRightToRunSparqlQuery()) {
            return jerseyResponseManager.unauthorized();
        }

        Query query;

        try{
            queryString = LDHelper.prefix + queryString;
            query = QueryFactory.create(queryString);
        } catch(QueryParseException ex) {
            return Response.status(400).build();
        }

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query);

        try {
            OutputStream outs = new ByteArrayOutputStream();
            ResultSet results = qexec.execSelect();
            ResultSetFormatter.outputAsJSON(outs,results);

            return  Response
                    .ok(outs.toString(), "application/sparql-results+json")
                    .build();

        } catch(QueryException ex) {
            return Response.status(500).build();
        } finally {
            qexec.close();
        }
    }

    @POST
    @ApiOperation(value = "Sends SPARQL Update query to given service", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Graph is saved"),
            @ApiResponse(code = 400, message = "Invalid graph supplied"),
            @ApiResponse(code = 403, message = "Illegal graph parameter"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 500, message = "Bad data?")
    })

    public Response sparqlUpdate(@ApiParam(value = "Sparql query", required = true) String body) {


        if(!authorizationManager.hasRightToRunSparqlQuery()) {
            return jerseyResponseManager.unauthorized();
        }

        String query = LDHelper.prefix + body;

        try {
            UpdateRequest queryObj= UpdateFactory.create(query);
            UpdateProcessor qexec= UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
            qexec.execute();


        } catch (UpdateException | QueryParseException ex) {
            return Response.status(400).build();
        }

        return Response.status(200).build();
    }
}
