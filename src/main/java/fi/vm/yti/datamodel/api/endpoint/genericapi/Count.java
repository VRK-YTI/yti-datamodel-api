package fi.vm.yti.datamodel.api.endpoint.genericapi;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("v1/system/counts")
@Tag(name = "System")
public class Count {

    private final EndpointServices endpointServices;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(Count.class.getName());

    @Autowired
    Count(EndpointServices endpointServices,
          ObjectMapper objectMapper) {
        this.endpointServices = endpointServices;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces("application/json")
    @Operation(description = "Counts objects in database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Query parse error"),
        @ApiResponse(responseCode = "500", description = "Query exception"),
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response counts() {

        String queryString = LDHelper.prefix + "SELECT ?type (COUNT(?g) as ?gc) " +
            "WHERE { " +
            "GRAPH ?g { " +
            "?g a ?type . " +
            "VALUES ?type { " +
            "dcap:DCAP dcap:MetadataVocabulary " +
            "rdfs:Class sh:NodeShape " +
            "owl:ObjectProperty owl:DatatypeProperty } " +
            "}} GROUP BY ?type";

        Query query = QueryFactory.create(queryString);

        Map<String, String> mapping = new HashMap<>();
        mapping.put(LDHelper.curieToURI("dcap:DCAP"),"profiles");
        mapping.put(LDHelper.curieToURI("dcap:MetadataVocabulary"),"libraries");
        mapping.put(LDHelper.curieToURI("rdfs:Class"),"classes");
        mapping.put(LDHelper.curieToURI("sh:NodeShape"),"shapes");
        mapping.put(LDHelper.curieToURI("owl:ObjectProperty"),"associations");
        mapping.put(LDHelper.curieToURI("owl:DatatypeProperty"),"attributes");

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), query)) {
            ResultSet results = qexec.execSelect();

            Map<String, String> object = new HashMap<>();

            while(results.hasNext()) {
                QuerySolution soln = results.next();
                String key = mapping.get(soln.get("type").asResource().getURI());
                String val = soln.get("gc").asLiteral().getString();
                object.put(key,val);
            }

            return Response
                .ok(objectMapper.writeValueAsString(object), "application/json")
                .build();

        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            return Response.status(500).build();
        }
    }

}
