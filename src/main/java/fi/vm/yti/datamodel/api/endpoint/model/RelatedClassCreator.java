/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusableClass;
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
@Path("v1/relatedClassCreator")
@Tag(name = "Class" )
public class RelatedClassCreator {

    private static final Logger logger = LoggerFactory.getLogger(RelatedClassCreator.class.getName());
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;

    @Autowired
    RelatedClassCreator(IDManager idManager,
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
    @Operation(description = "Create new class")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response newClass(
        @Parameter(description = "Model ID", required = true) @QueryParam("modelID") String modelID,
        @Parameter(description = "Old class id", required = true) @QueryParam("oldClass") String oldClass,
        @Parameter(description = "Relation type", required = true, schema = @Schema(allowableValues = {"rdfs:subClassOf", "iow:superClassOf", "prov:wasDerivedFrom"})) @QueryParam("relationType") String relationType) {

        IRI modelIRI, oldClassIRI;

        try {
            oldClassIRI = idManager.constructIRI(oldClass);
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

            ReusableClass newClass = new ReusableClass(oldClassIRI, modelIRI, LDHelper.curieToProperty(relationType), graphManager);

            return jerseyClient.constructResponseFromGraph(newClass.asGraph());
        } catch (IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
