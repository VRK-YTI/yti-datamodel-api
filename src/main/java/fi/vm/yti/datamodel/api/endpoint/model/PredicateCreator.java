/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.annotations.*;

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
@Path("predicateCreator")
@Api(tags = { "Predicate" }, description = "Creates new RDF properties that can be based on SKOS concepts")
public class PredicateCreator {

    private static final Logger logger = LoggerFactory.getLogger(PredicateCreator.class.getName());

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final GraphManager graphManager;
    private final TermedTerminologyManager termedTerminologyManager;

    @Autowired
    PredicateCreator(IDManager idManager,
                     JerseyResponseManager jerseyResponseManager,
                     JerseyClient jerseyClient,
                     GraphManager graphManager,
                     TermedTerminologyManager termedTerminologyManager) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.graphManager = graphManager;
        this.termedTerminologyManager = termedTerminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new predicate", notes = "Create new predicate")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New predicate is created"),
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
        @ApiResponse(code = 404, message = "Service not found") })

    public Response newPredicate(
        @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
        @ApiParam(value = "Predicate label", required = true) @QueryParam("predicateLabel") String predicateLabel,
        @ApiParam(value = "Concept ID") @QueryParam("conceptID") String conceptID,
        @ApiParam(value = "Predicate type", required = true, allowableValues = "owl:DatatypeProperty,owl:ObjectProperty") @QueryParam("type") String type,
        @ApiParam(value = "Language", required = true, allowableValues = "fi,en") @QueryParam("lang") String lang) {

        IRI conceptIRI = null;
        IRI modelIRI, typeIRI;
        try {
            String typeURI = type.replace("owl:", "http://www.w3.org/2002/07/owl#");
            if (conceptID != null && idManager.isValidUrl(conceptID)) {
                conceptIRI = idManager.constructIRI(conceptID);
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
                newPredicate = new ReusablePredicate(conceptIRI, modelIRI, predicateLabel, lang, typeIRI, graphManager, termedTerminologyManager);
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
