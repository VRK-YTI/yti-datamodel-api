/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Path("v1/predicate")
@Tag(name = "Predicate" )
public class Predicate {

    private static final Logger logger = LoggerFactory.getLogger(Predicate.class.getName());
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

    @Autowired
    Predicate(AuthorizationManager authorizationManager,
              AuthenticatedUserProvider userProvider,
              EndpointServices endpointServices,
              JerseyClient jerseyClient,
              JerseyResponseManager jerseyResponseManager,
              NamespaceManager namespaceManager,
              IDManager idManager,
              GraphManager graphManager,
              ProvenanceManager provenanceManager,
              ModelManager modelManager,
              SearchIndexManager searchIndexManager) {

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
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get property from model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getPredicate(
        @Parameter(description = "Property id") @QueryParam("id") String id,
        @Parameter(description = "Model id") @QueryParam("model") String model,
        @Parameter(description = "Required by model id") @QueryParam("requiredBy") String requiredBy) {

        if (id == null || id.equals("undefined") || id.equals("default")) {

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            // TODO: Create namespacemap from models
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            String queryString = "";

            if (model != null && !model.equals("undefined")) {
                queryString = QueryLibrary.listPredicatesQuery;
                pss.setIri("library", model);
                pss.setIri("hasPartGraph", model + "#HasPartGraph");
            } else {
                    if (requiredBy != null && !requiredBy.equals("undefined")) {
                        queryString = QueryLibrary.requiredPredicateQuery;
                        pss.setIri("library", requiredBy);
                    } else {
                        queryString = QueryLibrary.listPredicatesQuery;
                    }
                }

            pss.setCommandText(queryString);

            return jerseyClient.constructGraphFromServiceWithNamespaces(pss.toString(), endpointServices.getCoreSparqlAddress());

        } else {

            if (idManager.isInvalid(id)) {
                return jerseyResponseManager.invalidIRI();
            }

            if (id.startsWith("urn:")) {
                return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */

            Map<String, String> namespaceMap = namespaceManager.getCoreNamespaceMap(id);

            if (namespaceMap == null) {
                logger.info("No model for " + id);
                return jerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.predicateQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            if (model != null && !model.equals("undefined")) {
                pss.setIri("library", model);
            }

            return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());

        }

    }

    @POST
    @Operation(description = "Create new property to certain model OR add reference from existing property to another model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Graph is created"),
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response postPredicate(
        @Parameter(description = "New graph in application/ld+json", required = false)
            String body,
        @Parameter(description = "Property ID", required = true)
        @QueryParam("id")
            String id,
        @Parameter(description = "OLD Property ID")
        @QueryParam("oldid")
            String oldid,
        @Parameter(description = "Model ID", required = true)
        @QueryParam("model")
            String model) {

        try {

            IRI modelIRI, idIRI, oldIdIRI = null;

            /* Check that URIs are valid */
            try {
                modelIRI = idManager.constructIRI(model);
                idIRI = idManager.constructIRI(id);
                /* If oldid exists */
                if (oldid != null && !oldid.equals("undefined")) {
                    if (oldid.equals(id)) {
                        /* id and oldid cant be the same */
                        return jerseyResponseManager.usedIRI();
                    }
                    oldIdIRI = idManager.constructIRI(oldid);
                }
            } catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }

            String provUUID = null;

            if (isNotEmpty(body)) {

                YtiUser user = userProvider.getUser();

                Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

                if (parsedModel.size() == 0) {
                    return jerseyResponseManager.notAcceptable();
                }

                ReusablePredicate updatePredicate = new ReusablePredicate(parsedModel, graphManager);

                if (!authorizationManager.hasRightToEdit(updatePredicate)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* Rename ID if oldIdIRI exists */
                if (oldIdIRI != null) {
                    /* Prevent overwriting existing resources */
                    if (graphManager.isExistingGraph(idIRI)) {
                        logger.warn(idIRI + " is existing graph!");
                        return jerseyResponseManager.usedIRI();
                    } else {
                        ReusablePredicate oldPredicate = new ReusablePredicate(oldIdIRI,graphManager);

                        if(!oldPredicate.getStatus().equals(updatePredicate.getStatus())) {
                            updatePredicate.setStatusModified();
                        }

                        graphManager.updateResourceWithNewId(updatePredicate, oldPredicate);
                        provUUID = updatePredicate.getProvUUID();
                        logger.info("Changed predicate id from:" + oldid + " to " + id);
                        searchIndexManager.removePredicate(oldid);
                        searchIndexManager.createIndexPredicate(updatePredicate);
                    }
                } else {
                    ReusablePredicate oldPredicate = new ReusablePredicate(idIRI,graphManager);

                    if(!oldPredicate.getStatus().equals(updatePredicate.getStatus())) {
                        updatePredicate.setStatusModified();
                    }

                    graphManager.updateResource(updatePredicate, oldPredicate);
                    logger.info("Updated " + updatePredicate.getId());
                    provUUID = updatePredicate.getProvUUID();
                    searchIndexManager.updateIndexPredicate(updatePredicate);
                }

                searchIndexManager.updateIndexModel(updatePredicate.getModelId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvEntityBundle(updatePredicate.getId(), updatePredicate.asGraph(), user.getId(), updatePredicate.getProvUUID(), oldIdIRI);
                }

            } else {

                if (!authorizationManager.hasRightToAddPredicateReference(modelIRI, id)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
                if (LDHelper.isResourceDefinedInNamespace(id, model)) {
                    // Selfreferences not allowed
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
            } else return jerseyResponseManager.notCreated();

        } catch (IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch (RiotException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.notAcceptable();
        }
    }

    @PUT
    @Operation(description = "Create new property to certain model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Graph is created"),
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response putPredicate(
        @Parameter(description = "New graph in application/ld+json", required = true) String body) {

        try {

            Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

            if (parsedModel.size() == 0) {
                return jerseyResponseManager.notAcceptable();
            }

            ReusablePredicate newPredicate = new ReusablePredicate(parsedModel, graphManager);
            YtiUser user = userProvider.getUser();

            /* Prevent overwriting existing predicate */
            if (graphManager.isExistingGraph(newPredicate.getId())) {
                logger.warn(newPredicate.getId() + " is existing predicate!");
                return jerseyResponseManager.usedIRI();
            }

            if (!authorizationManager.hasRightToEdit(newPredicate)) {
                return jerseyResponseManager.unauthorized();
            }

            String provUUID = newPredicate.getProvUUID();
            graphManager.createResource(newPredicate);

            searchIndexManager.createIndexPredicate(newPredicate);
            searchIndexManager.updateIndexModel(newPredicate.getModelId());

            if (provenanceManager.getProvMode()) {
                provenanceManager.createProvenanceActivityFromModel(newPredicate.getId(), newPredicate.asGraph(), newPredicate.getProvUUID(), user.getId());
            }

            if (provUUID != null) {
                return jerseyResponseManager.successUrnUuid(provUUID, newPredicate.getId());
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

    @DELETE
    @Operation(description = "Delete predicate graph or reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Graph is deleted"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "No such graph"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response deletePredicate(
        @Parameter(description = "Model ID", required = true)
        @QueryParam("model") String model,
        @Parameter(description = "Predicate ID", required = true)
        @QueryParam("id") String id) {

        /* Check that URIs are valid */
        IRI modelIRI, idIRI;
        try {
            modelIRI = idManager.constructIRI(model);
            idIRI = idManager.constructIRI(id);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        YtiUser user = userProvider.getUser();

        /* If Predicate is defined in the model */
        if (id.startsWith(model)) {
            /* Remove graph */

            try {
                logger.info("Removing " + idIRI.toString());
                ReusablePredicate deletePredicate = new ReusablePredicate(idIRI, graphManager);

                if (!authorizationManager.hasRightToEdit(deletePredicate)) {
                    return jerseyResponseManager.unauthorized();
                }

                graphManager.deleteResource(deletePredicate);
                searchIndexManager.removePredicate(id);
                searchIndexManager.updateIndexModel(deletePredicate.getModelId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.invalidateProvenanceActivity(deletePredicate.getId(), deletePredicate.getProvUUID(), user.getId());
                }

            } catch (IllegalArgumentException ex) {
                logger.warn(ex.toString());
                return jerseyResponseManager.unexpected();
            }
            return jerseyResponseManager.ok();
        } else {

            if (!authorizationManager.hasRightToRemovePredicateReference(modelIRI, idIRI)) {
                return jerseyResponseManager.unauthorized();
            }

            /* If removing referenced predicate */
            logger.debug("Deleting referenced class "+idIRI.toString()+ "from "+modelIRI.toString());
            graphManager.deleteGraphReferenceFromExportModel(idIRI, modelIRI);
            graphManager.deletePositionGraphReferencesFromModel(model,id);
            graphManager.updateContentModified(model);

            return jerseyResponseManager.ok();
        }
    }
}
