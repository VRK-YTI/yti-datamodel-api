/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Path("v1/class")
@Tag(name = "Class" )
public class Class {

    private static final Logger logger = LoggerFactory.getLogger(Class.class.getName());

    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;
    private final NamespaceManager namespaceManager;
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final ProvenanceManager provenanceManager;
    private final ModelManager modelManager;
    private final SearchIndexManager searchIndexManager;
    private final ObjectMapper objectMapper;

    @Autowired
    Class(AuthorizationManager authorizationManager,
          AuthenticatedUserProvider userProvider,
          EndpointServices endpointServices,
          JerseyClient jerseyClient,
          JerseyResponseManager jerseyResponseManager,
          NamespaceManager namespaceManager,
          IDManager idManager,
          GraphManager graphManager,
          ProvenanceManager provenanceManager,
          ModelManager modelManager,
          SearchIndexManager searchIndexManager,
          ObjectMapper objectMapper) {

        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
        this.namespaceManager = namespaceManager;
        this.idManager = idManager;
        this.graphManager = graphManager;
        this.provenanceManager = provenanceManager;
        this.modelManager = modelManager;
        this.searchIndexManager = searchIndexManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get class from model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "404", description = "No such resource"),
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getClass(
        @Parameter(description = "Class id") @QueryParam("id") String id,
        @Parameter(description = "Model id") @QueryParam("model") String model) {

        if (id == null || id.equals("undefined") || id.equals("default")) {

            /* If no id is provided create a list of classes */
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            // TODO: Create namespacemap from all models
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            String queryString = QueryLibrary.listClassesQuery;

            if (model != null && !model.equals("undefined")) {
                pss.setIri("library", model);
                pss.setIri("hasPartGraph", model + "#HasPartGraph");
            }

            pss.setCommandText(queryString);

            return jerseyClient.constructGraphFromServiceWithNamespaces(pss.toString(), endpointServices.getCoreSparqlAddress());

        } else {

            if (!idManager.isValidUrl(id)) {
                return jerseyResponseManager.invalidIRI();
            }

            if (id.startsWith("urn:")) {
                return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */

            Map<String, String> namespaceMap = namespaceManager.getCoreNamespaceMap(id);

            if (namespaceMap == null) {
                return jerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.classQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            if (model != null && !model.equals("undefined")) {
                pss.setIri("library", model);
            }

            return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());
        }
    }

    @POST
    @Operation(description = "Update class in certain model OR add reference from existing class to another model AND/OR change Class ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Graph is created"),
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response postClass(
        @Parameter(description = "New graph in application/ld+json", required = false) String body,
        @Parameter(description = "Class ID", required = true)
        @QueryParam("id") String id,
        @Parameter(description = "OLD Class ID")
        @QueryParam("oldid") String oldid,
        @Parameter(description = "Model ID", required = true)
        @QueryParam("model") String model) {

        try {

            IRI modelIRI, idIRI, oldIdIRI = null;

            /* Check that URIs are valid */
            try {
                modelIRI = idManager.constructIRI(model);
                idIRI = idManager.constructIRI(id);
                /* If newid exists */
                if (oldid != null && !oldid.equals("undefined")) {
                    if (oldid.equals(id)) {
                        /* id and newid cant be the same */
                        return jerseyResponseManager.usedIRI();
                    }
                    oldIdIRI = idManager.constructIRI(oldid);
                }
            } catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }

            String provUUID = null;

            if (isNotEmpty(body)) {

                Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

                if (parsedModel.size() == 0) {
                    return jerseyResponseManager.notAcceptable();
                }

                ReusableClass updateClass = new ReusableClass(parsedModel, graphManager);
                YtiUser user = userProvider.getUser();

                if (!authorizationManager.hasRightToEdit(updateClass)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* Rename ID if oldIdIRI exists */
                if (oldIdIRI != null) {
                    /* Prevent overwriting existing resources */
                    if (graphManager.isExistingGraph(idIRI)) {
                        logger.warn(idIRI + " is existing graph!");
                        return jerseyResponseManager.usedIRI();
                    } else {
                        ReusableClass oldClass = new ReusableClass(oldIdIRI, graphManager);

                        if(!oldClass.getStatus().equals(updateClass.getStatus())) {
                            updateClass.setStatusModified();
                        }

                        if (graphManager.modelStatusRestrictsRemoving(oldIdIRI)) {
                            logger.warn(idIRI + " is existing graph!");
                            return jerseyResponseManager.depedencies();
                        } else {
                            graphManager.updateResourceWithNewId(updateClass, oldClass);
                            provUUID = updateClass.getProvUUID();
                            logger.info("Changed class id from:" + oldid + " to " + id);
                            searchIndexManager.removeClass(oldid);
                            searchIndexManager.createIndexClass(updateClass);
                        }
                    }
                } else {
                    ReusableClass oldClass = new ReusableClass(idIRI, graphManager);

                    if(!oldClass.getStatus().equals(updateClass.getStatus())) {
                        updateClass.setStatusModified();
                    }

                    graphManager.updateResource(updateClass,oldClass);
                    provUUID = updateClass.getProvUUID();
                    searchIndexManager.updateIndexClass(updateClass);
                }

                searchIndexManager.updateIndexModel(updateClass.getModelId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvEntityBundle(updateClass.getId(), updateClass.asGraph(), user.getId(), updateClass.getProvUUID(), oldIdIRI);
                }

            } else {
                /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */

                if (!authorizationManager.hasRightToAddClassReference(modelIRI, id)) {
                    return jerseyResponseManager.unauthorized();
                }

                if (LDHelper.isResourceDefinedInNamespace(id, model)) {
                    // Self references not allowed
                    return jerseyResponseManager.usedIRI();
                } else {
                    graphManager.insertExistingResourceToModel(id, model);
                    graphManager.updateContentModified(model);
                    logger.info("Created reference from " + model + " to " + id);
                    return jerseyResponseManager.ok();
                }
            }

            if (provUUID != null) {
                return jerseyResponseManager.successUuid(provUUID);
            } else {
                return jerseyResponseManager.notCreated();
            }

        } catch (IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch (RiotException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.notAcceptable();
        }
    }

    @PUT
    @Operation(description = "Create new class to certain model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Graph is created"),
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response putClass(
        @Parameter(description = "New graph in application/ld+json", required = true) String body) {

        try {

            Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

            if (parsedModel.size() == 0) {
                return jerseyResponseManager.notAcceptable();
            }

            ReusableClass newClass = new ReusableClass(parsedModel, graphManager);
            YtiUser user = userProvider.getUser();

            if (!authorizationManager.hasRightToEdit(newClass)) {
                return jerseyResponseManager.unauthorized();
            }

            /* Prevent overwriting existing classes */
            if (graphManager.isExistingGraph(newClass.getId())) {
                logger.warn(newClass.getId() + " is existing class!");
                return jerseyResponseManager.usedIRI();
            }

            String provUUID = newClass.getProvUUID();

            if (provUUID == null) {
                return jerseyResponseManager.serverError();
            } else {
                graphManager.createResource(newClass);
                logger.info("Created " + newClass.getId());

                searchIndexManager.createIndexClass(newClass);
                searchIndexManager.updateIndexModel(newClass.getModelId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvenanceActivityFromModel(newClass.getId(), newClass.asGraph(), newClass.getProvUUID(), user.getId());
                }

                return jerseyResponseManager.successUrnUuid(newClass.getProvUUID(), newClass.getId());
            }

        } catch (IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.invalidParameter();
        }
    }

    @DELETE
    @Operation(description = "Delete graph from service and service description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Graph is deleted"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "No such graph"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response deleteClass(
        @Parameter(description = "Model ID", required = true)
        @QueryParam("model") String model,
        @Parameter(description = "Class ID", required = true)
        @QueryParam("id") String id) {

        /* Check that URIs are valid */
        IRI modelIRI, idIRI;
        try {
            modelIRI = idManager.constructIRI(model);
            idIRI = idManager.constructIRI(id);
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidIRI();
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        YtiUser user = userProvider.getUser();

        /* If Class is defined in the model */
        if (id.startsWith(model)) {
            /* Remove graph */

            try {
                ReusableClass deleteClass = new ReusableClass(idIRI, graphManager);

                if (!authorizationManager.hasRightToEdit(deleteClass)) {
                    return jerseyResponseManager.unauthorized();
                }

                graphManager.deleteResource(deleteClass);
                searchIndexManager.removeClass(id);
                searchIndexManager.updateIndexModel(deleteClass.getModelId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.invalidateProvenanceActivity(deleteClass.getId(), deleteClass.getProvUUID(), user.getId());
                }

            } catch (IllegalArgumentException ex) {
                logger.warn(ex.toString());
                return jerseyResponseManager.unexpected();
            }

            return jerseyResponseManager.ok();
        } else {

            if (!authorizationManager.hasRightToRemoveClassReference(modelIRI, idIRI)) {
                return jerseyResponseManager.unauthorized();
            }

            /* If removing referenced class */
            logger.debug("Deleting referenced class "+idIRI.toString()+ "from "+modelIRI.toString());
            graphManager.deleteGraphReferenceFromExportModel(idIRI, modelIRI);
            graphManager.deletePositionGraphReferencesFromModel(model,id);
            graphManager.updateContentModified(model);

            return jerseyResponseManager.ok();
        }
    }

}
