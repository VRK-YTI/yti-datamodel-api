package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.UUID;

import fi.vm.yti.datamodel.api.index.FrameManager;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.security.AuthorizationManagerImpl;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

// Create new version of the model with new prefix

@Component
@Path("v1/modelVersion")
@Tag(name = "Model" )
public class ModelVersion {

    private static final Logger logger = LoggerFactory.getLogger(Models.class.getName());
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final FrameManager frameManager;
    private final ServiceDescriptionManager serviceDescriptionManager;
    private final SearchIndexManager searchIndexManager;
    private final ProvenanceManager provenanceManager;

    @Autowired
    ModelVersion(AuthorizationManager authorizationManager,
                 AuthenticatedUserProvider userProvider,
                 GraphManager graphManager,
                 FrameManager frameManager,
                 ServiceDescriptionManager serviceDescriptionManager,
                 IDManager idManager,
                 JerseyResponseManager jerseyResponseManager,
                 SearchIndexManager searchIndexManager,
                 ProvenanceManager provenanceManager) {
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.idManager = idManager;
        this.graphManager = graphManager;
        this.frameManager = frameManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.serviceDescriptionManager = serviceDescriptionManager;
        this.searchIndexManager = searchIndexManager;
        this.provenanceManager = provenanceManager;
    }

    @POST
    @Operation(description = "Create new version from model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Change made"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })

    public Response newVersion(
        @Parameter(description = "New prefix") @QueryParam("newPrefix") String newPrefix,
        @Parameter(description = "Model URI") @QueryParam("uri") String id) {

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

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvenanceActivityFromModel(newModelIRI.toString(), dataModel, "urn:uuid:"+UUID.randomUUID().toString(), user.getId());
                    provenanceManager.createProvenanceActivityForNewVersionModel(newModelIRI.toString(), user.getId());
                }

                searchIndexManager.createIndexModel(newId);
                searchIndexManager.initClassIndexFromModel(newId);
                searchIndexManager.initPredicateIndexFromModel(newId);

                dataModel.add(frameManager.constructExportGraph(newModelIRI.toString()));
                graphManager.putToGraph(dataModel, newModelIRI.toString() + "#ExportGraph");

                logger.info("Created export graph");

            }
        }

        return jerseyResponseManager.successUri(newModelIRI.toString());
    }
}
