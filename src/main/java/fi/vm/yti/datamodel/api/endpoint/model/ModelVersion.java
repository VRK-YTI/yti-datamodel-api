package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.index.FrameManager;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.*;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Path("v1/modelVersion")
@Api(tags = { "Model" }, description = "Create new version of the model with new prefix")
public class ModelVersion {

    private static final Logger logger = LoggerFactory.getLogger(Models.class.getName());
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final FrameManager frameManager;
    private final ServiceDescriptionManager serviceDescriptionManager;

    @Autowired
    ModelVersion(AuthorizationManager authorizationManager,
                 AuthenticatedUserProvider userProvider,
                 GraphManager graphManager,
                 FrameManager frameManager,
                 ServiceDescriptionManager serviceDescriptionManager,
                 IDManager idManager,
                 JerseyResponseManager jerseyResponseManager) {
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.idManager = idManager;
        this.graphManager = graphManager;
        this.frameManager = frameManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.serviceDescriptionManager = serviceDescriptionManager;
    }

    @POST
    @ApiOperation(value = "Renames prefix using fixed SPARQL update", notes = "HAZARD operation")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Change made"),
        @ApiResponse(code = 400, message = "Invalid graph supplied"),
        @ApiResponse(code = 403, message = "Illegal graph parameter"),
        @ApiResponse(code = 404, message = "Service not found")
    })

    public Response newVersion(
        @ApiParam(value = "New prefix")
        @QueryParam("newPrefix") String newPrefix,
        @ApiParam(value = "Model URI")
        @QueryParam("uri") String id) {

        IRI modelIRI;
        YtiUser user = userProvider.getUser();

        try {
            modelIRI = idManager.constructIRI(id);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        DataModel oldVocabulary;

        try {
            oldVocabulary = new DataModel(modelIRI, graphManager);
        } catch (IllegalArgumentException ex) {
            return jerseyResponseManager.invalidParameter();
        }

        if (!authorizationManager.hasRightToCreateNewVersion(oldVocabulary)) {
            return jerseyResponseManager.unauthorized();
        }

        String oldPrefix = oldVocabulary.getPrefix();

        if (oldPrefix.equals(newPrefix)) {
            logger.info("New prefix and old prefix cannot be the same");
            return jerseyResponseManager.invalidParameter();
        }

        String newId = id.replaceAll(oldPrefix + "$", newPrefix);
        logger.info("New id: " + newId);

        IRI newModelIRI;

        try {
            newModelIRI = idManager.constructIRI(newId);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        if (newPrefix != null && newPrefix.length() > 1) {
            logger.info("Creating new version from " + oldVocabulary.getIRI());
            if (graphManager.isExistingGraph(newModelIRI)) {
                logger.info("Model with the given prefix already exist!");
                return jerseyResponseManager.usedIRI();
            } else {

                logger.info("Creating new version from " + id + " to " + newId);
                Model dataModel = oldVocabulary.asGraph();

                graphManager.newModelVersion(dataModel, newPrefix, modelIRI, newModelIRI);

                serviceDescriptionManager.createGraphDescription(newId, user.getId(), oldVocabulary.getOrganizations());

                logger.info("Created new model");

                dataModel.add(frameManager.constructExportGraph(newModelIRI.toString()));
                graphManager.putToGraph(dataModel, newModelIRI.toString() + "#ExportGraph");

                logger.info("Created export graph");

            }
        }

        return Response.status(200).build();
    }
}
