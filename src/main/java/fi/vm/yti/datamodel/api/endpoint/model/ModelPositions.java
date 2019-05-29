/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.annotations.*;

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
@Path("modelPositions")
@Api(tags = { "Model" }, description = "Operations about coordinates")
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
    @ApiOperation(value = "Get model from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "Graph id", defaultValue = "default")
        @QueryParam("model") String model) {

        return jerseyClient.getNonEmptyGraphResponseFromService(model + "#PositionGraph", endpointServices.getCoreReadAddress(), "application/ld+json", false);

    }

    @PUT
    @ApiOperation(value = "Updates model coordinates", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Graph is created"),
        @ApiResponse(code = 204, message = "Graph is saved"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 405, message = "Update not allowed"),
        @ApiResponse(code = 403, message = "Illegal graph parameter"),
        @ApiResponse(code = 400, message = "Invalid graph supplied"),
        @ApiResponse(code = 500, message = "Bad data?")
    })
    public Response putJson(
        @ApiParam(value = "New graph in application/ld+json", required = true)
            String body,
        @ApiParam(value = "Model ID", required = true)
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

        jenaClient.putModelToCore(model + "#PositionGraph", newPositions);

        return jerseyResponseManager.okEmptyContent();
    }
}
