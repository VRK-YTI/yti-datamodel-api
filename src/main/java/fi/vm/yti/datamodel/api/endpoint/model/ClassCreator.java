/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusableClass;
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
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

@Component
@Path("classCreator")
@Api(tags = {"Class"}, description = "Construct new Class template")
public class ClassCreator {

    private static final Logger logger = LoggerFactory.getLogger(ClassCreator.class.getName());
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;
    private final TermedTerminologyManager termedTerminologyManager;

    @Autowired
    ClassCreator(IDManager idManager,
                 GraphManager graphManager,
                 JerseyResponseManager jerseyResponseManager,
                 JerseyClient jerseyClient,
                 JenaClient jenaClient,
                 ModelManager modelManager,
                 TermedTerminologyManager termedTerminologyManager) {

        this.idManager = idManager;
        this.graphManager = graphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
        this.termedTerminologyManager = termedTerminologyManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
            @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Class label", required = true) @QueryParam("classLabel") String classLabel,
            @ApiParam(value = "Concept ID") @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

        IRI conceptIRI = null;
        IRI modelIRI;

        try {

            if(conceptID!=null && idManager.isValidUrl(conceptID)) {
                logger.info("Using concept "+conceptID);
                conceptIRI = idManager.constructIRI(conceptID);
            } else {
                if(conceptID!=null) {
                    logger.warn("Concept is not URI: "+conceptID);
                }
            }

            modelIRI = idManager.constructIRI(modelID);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        }

        if(!graphManager.isExistingGraph(modelIRI)) {
            return jerseyResponseManager.notFound();
        }

        try {

            ReusableClass newClass;

            if (conceptIRI != null) {
                newClass = new ReusableClass(conceptIRI, modelIRI, classLabel, lang, graphManager, jenaClient, modelManager, termedTerminologyManager);
            } else {
                newClass = new ReusableClass(modelIRI, classLabel, lang, graphManager);
            }

            return jerseyClient.constructResponseFromGraph(newClass.asGraph());
        } catch(IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
