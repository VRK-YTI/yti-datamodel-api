/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.security.AuthorizationManagerImpl;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/modelPositions")
@Tag(name = "Model")
public class ModelPositions {

    private static final Logger logger = LoggerFactory.getLogger(ModelPositions.class.getName());

    private final AuthorizationManager authorizationManager;
    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;
    private final EndpointServices endpointServices;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;
    private final IDManager idManager;
    private final GraphManager graphManager;

    @Autowired
    ModelPositions(AuthorizationManager authorizationManager,
                   JerseyClient jerseyClient,
                   JerseyResponseManager jerseyResponseManager,
                   EndpointServices endpointServices,
                   JenaClient jenaClient,
                   ModelManager modelManager,
                   IDManager idManager,
                   GraphManager graphManager) {

        this.authorizationManager = authorizationManager;
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
        this.endpointServices = endpointServices;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
        this.idManager = idManager;
        this.graphManager = graphManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get model from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getModelPositions(
        @Parameter(description = "Graph id", schema = @Schema(defaultValue = "default"))
        @QueryParam("model") String model) {

        return jerseyClient.getNonEmptyGraphResponseFromService(model + "#PositionGraph", endpointServices.getCoreReadAddress(), "application/ld+json", false);

    }

    @PUT
    @Operation(description = "Updates model coordinates")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Graph is created"),
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response putModelPositions(
        @Parameter(description = "New graph in application/ld+json", required = true)
            String body,
        @Parameter(description = "Model ID", required = true)
        @QueryParam("model")
            String model) {

        if (model.equals("default")) {
            return jerseyResponseManager.invalidIRI();
        }

        IRI modelIRI;

        try {
            modelIRI = idManager.constructIRI(model);
        } catch (IRIException e) {
            logger.warn("GRAPH ID is invalid IRI!");
            return jerseyResponseManager.invalidIRI();
        }

        DataModel checkModel = new DataModel(modelIRI, graphManager);

        if (!authorizationManager.hasRightToEdit(checkModel)) {
            return jerseyResponseManager.unauthorized();
        }

        Model newPositions = modelManager.createJenaModelFromJSONLDString(body);

        if (newPositions.size() < 1) {
            return jerseyResponseManager.invalidParameter();
        }

        // TODO: Does this fix the strange duplication bug?
        jenaClient.deleteModelFromCore(model+"#PositionGraph");

        jenaClient.putModelToCore(model + "#PositionGraph", newPositions);

        return jerseyResponseManager.okEmptyContent();
    }
}
