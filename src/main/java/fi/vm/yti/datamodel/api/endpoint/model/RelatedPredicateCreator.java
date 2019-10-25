/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/relatedPredicateCreator")
@Tag(name = "Predicate" )
public class RelatedPredicateCreator {

    private static final Logger logger = LoggerFactory.getLogger(RelatedPredicateCreator.class.getName());
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;

    @Autowired
    RelatedPredicateCreator(IDManager idManager,
                            GraphManager graphManager,
                            JerseyResponseManager jerseyResponseManager,
                            JerseyClient jerseyClient) {

        this.idManager = idManager;
        this.graphManager = graphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new super predicate")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New super predicate is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response createRelatedPredicate(
        @Parameter(description = "Model ID", required = true) @QueryParam("modelID") String modelID,
        @Parameter(description = "Old predicate id", required = true) @QueryParam("oldPredicate") String oldPredicate,
        @Parameter(description = "Relation type", required = true, schema = @Schema(allowableValues = "rdfs:subPropertyOf,iow:superPropertyOf,prov:wasDerivedFrom")) @QueryParam("relationType") String relationType) {

        IRI modelIRI, oldPredicateIRI;

        try {
            oldPredicateIRI = idManager.constructIRI(oldPredicate);
            modelIRI = idManager.constructIRI(modelID);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        }

        if (!graphManager.isExistingGraph(modelIRI)) {
            logger.debug("Graph not found!");
            return jerseyResponseManager.notFound();
        }

        try {

            ReusablePredicate newPredicate = new ReusablePredicate(oldPredicateIRI, modelIRI, LDHelper.curieToProperty(relationType), graphManager);

            return jerseyClient.constructResponseFromGraph(newPredicate.asGraph());
        } catch (IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
