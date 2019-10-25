/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/classCreator")
@Tag(name = "Class" )
public class ClassCreator {

    private static final Logger logger = LoggerFactory.getLogger(ClassCreator.class.getName());
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;
    private final TerminologyManager terminologyManager;

    @Autowired
    ClassCreator(IDManager idManager,
                 GraphManager graphManager,
                 JerseyResponseManager jerseyResponseManager,
                 JerseyClient jerseyClient,
                 JenaClient jenaClient,
                 ModelManager modelManager,
                 TerminologyManager terminologyManager) {

        this.idManager = idManager;
        this.graphManager = graphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
        this.terminologyManager = terminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new class")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response newClass(
        @Parameter(description = "Model URI", required = true) @QueryParam("modelID") String modelID,
        @Parameter(description = "Class label", required = true) @QueryParam("classLabel") String classLabel,
        @Parameter(description = "Concept URI") @QueryParam("conceptID") String conceptUri,
        @Parameter(description = "Language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang) {

        IRI conceptIRI = null;
        IRI modelIRI;

        try {

            if (conceptUri!= null && idManager.isValidUrl(conceptUri)) {
                logger.info("Using concept " + conceptUri);
                conceptIRI = idManager.constructIRI(conceptUri);
            } else {
                if (conceptUri != null) {
                    logger.warn("Concept is not URI: " + conceptUri);
                }
            }

            modelIRI = idManager.constructIRI(modelID);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        }

        if (!graphManager.isExistingGraph(modelIRI)) {
            return jerseyResponseManager.notFound();
        }

        try {

            ReusableClass newClass;

            if (conceptIRI != null) {
                newClass = new ReusableClass(conceptIRI, modelIRI, classLabel, lang, graphManager, terminologyManager);
            } else {
                newClass = new ReusableClass(modelIRI, classLabel, lang, graphManager);
            }

            return jerseyClient.constructResponseFromGraph(newClass.asGraph());
        } catch (IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
