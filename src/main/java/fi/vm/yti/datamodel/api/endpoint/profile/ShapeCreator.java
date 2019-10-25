/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.profile;

import fi.vm.yti.datamodel.api.model.Shape;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.util.SplitIRI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/shapeCreator")
@Tag(name = "Profile")
public class ShapeCreator {

    private static final Logger logger = LoggerFactory.getLogger(ShapeCreator.class.getName());

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final GraphManager graphManager;
    private final EndpointServices endpointServices;

    @Autowired
    ShapeCreator(IDManager idManager,
                 JerseyResponseManager jerseyResponseManager,
                 JerseyClient jerseyClient,
                 GraphManager graphManager,
                 EndpointServices endpointServices) {

        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.graphManager = graphManager;
        this.endpointServices = endpointServices;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new class")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response newClass(
        @Parameter(description = "Profile ID", required = true) @QueryParam("profileID") String profileID,
        @Parameter(description = "Class ID", required = true) @QueryParam("classID") String classID,
        @Parameter(description = "Language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang) {

        IRI classIRI, profileIRI, shapeIRI;
        try {
            classIRI = idManager.constructIRI(classID);
            profileIRI = idManager.constructIRI(profileID);
            if (profileID.endsWith("/") || profileID.endsWith("#")) {
                shapeIRI = idManager.constructIRI(profileIRI + SplitIRI.localname(classID));
            } else {
                shapeIRI = idManager.constructIRI(profileIRI + "#" + SplitIRI.localname(classID));
            }
        } catch (IRIException e) {
            logger.warn("ID is invalid IRI!");
            return jerseyResponseManager.invalidIRI();
        }

        try {
            Shape newShape = new Shape(classIRI, shapeIRI, profileIRI, graphManager, endpointServices);
            return jerseyClient.constructResponseFromGraph(newShape.asGraph());
        } catch (IllegalArgumentException e) {
            return jerseyResponseManager.invalidParameter();
        }

    }
}
