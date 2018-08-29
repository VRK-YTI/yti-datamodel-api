/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.service.*;
import io.swagger.annotations.*;
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
@Path("superClassCreator")
@Api(tags = {"Class"}, description = "Construct new SuperClass template")
public class SuperClassCreator {

    private static final Logger logger = LoggerFactory.getLogger(SuperClassCreator.class.getName());
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;
    private final TermedTerminologyManager termedTerminologyManager;

    @Autowired
    SuperClassCreator(IDManager idManager,
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
            @ApiParam(value = "Old class id", required = true) @QueryParam("oldClass") String oldClass) {

        IRI modelIRI, oldClassIRI;

        try {
            oldClassIRI = idManager.constructIRI(oldClass);
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

            ReusableClass newClass = new ReusableClass(oldClassIRI, modelIRI , graphManager);

            return jerseyClient.constructResponseFromGraph(newClass.asGraph());
        } catch(IllegalArgumentException ex) {
            logger.info(ex.toString());
            return jerseyResponseManager.error();
        }
    }
}
