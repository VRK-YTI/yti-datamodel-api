/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.ExternalGraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/externalPredicate")
@Tag(name = "Predicate")
public class ExternalPredicate {

    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final IDManager idManager;
    private final ExternalGraphManager externalGraphManager;

    @Autowired
    ExternalPredicate(ExternalGraphManager externalGraphManager,
                      JerseyResponseManager jerseyResponseManager,
                      JerseyClient jerseyClient,
                      IDManager idManager) {

        this.externalGraphManager = externalGraphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.idManager = idManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get external predicate from required model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "404", description = "No such resource"),
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getExternalPredicate(
        @Parameter(description = "Predicate id")
        @QueryParam("id") String id,
        @Parameter(description = "Model id")
        @QueryParam("model") String model) {

        IRI idIRI;

        /* Check that Model URI is valid */
        if (!idManager.isValidUrl(model)) {
            return jerseyResponseManager.invalidIRI();
        }

        if (id == null || id.equals("undefined") || id.equals("default")) {

            // IF id is null get list of external predicates
            return jerseyClient.constructResponseFromGraph(externalGraphManager.getListOfExternalPredicates(model));

        } else {

            try {
                idIRI = idManager.constructIRI(id);
            } catch (NullPointerException e) {
                return jerseyResponseManager.unexpected();
            } catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }

            return jerseyClient.constructResponseFromGraph(externalGraphManager.getExternalPredicate(idIRI, model));

        }
    }
}
