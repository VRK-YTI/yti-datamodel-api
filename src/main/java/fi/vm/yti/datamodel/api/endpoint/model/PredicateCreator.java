/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusablePredicate;
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

// Creates new RDF properties that can be based on SKOS concepts

@Component
@Path("v1/predicateCreator")
@Tag(name = "Predicate")
public class PredicateCreator {

    private static final Logger logger = LoggerFactory.getLogger(PredicateCreator.class.getName());

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final GraphManager graphManager;
    private final TerminologyManager terminologyManager;

    @Autowired
    PredicateCreator(IDManager idManager,
                     JerseyResponseManager jerseyResponseManager,
                     JerseyClient jerseyClient,
                     GraphManager graphManager,
                     TerminologyManager terminologyManager) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.graphManager = graphManager;
        this.terminologyManager = terminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new predicate")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New predicate is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })

    public Response createNewPredicate(
        @Parameter(description = "Model ID", required = true) @QueryParam("modelID") String modelID,
        @Parameter(description = "Predicate label", required = true) @QueryParam("predicateLabel") String predicateLabel,
        @Parameter(description = "Concept URI") @QueryParam("conceptID") String conceptUri,
        @Parameter(description = "Predicate type", required = true, schema = @Schema(allowableValues = "owl:DatatypeProperty,owl:ObjectProperty")) @QueryParam("type") String type,
        @Parameter(description = "Language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang) {

        IRI conceptIRI = null;
        IRI modelIRI, typeIRI;
        try {
            String typeURI = type.replace("owl:", "http://www.w3.org/2002/07/owl#");
            if (conceptUri != null && idManager.isValidUrl(conceptUri)) {
                conceptIRI = idManager.constructIRI(conceptUri);
            }
            modelIRI = idManager.constructIRI(modelID);
            typeIRI = idManager.constructIRI(typeURI);
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        try {
            ReusablePredicate newPredicate;

            if (conceptIRI != null) {
                newPredicate = new ReusablePredicate(conceptIRI, modelIRI, predicateLabel, lang, typeIRI, graphManager, terminologyManager);
            } else {
                newPredicate = new ReusablePredicate(modelIRI, predicateLabel, lang, typeIRI, graphManager);
            }

            return jerseyClient.constructResponseFromGraph(newPredicate.asGraph());
        } catch (IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
