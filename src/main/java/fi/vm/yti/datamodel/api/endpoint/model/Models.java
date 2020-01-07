/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.Set;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.service.ProvenanceManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.datamodel.api.service.ServiceDescriptionManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("v1/model")
@Tag(name = "Model" )
public class Models {

    private static final Logger logger = LoggerFactory.getLogger(Models.class.getName());

    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final EndpointServices endpointServices;
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final IDManager idManager;
    private final JerseyClient jerseyClient;
    private final ServiceDescriptionManager serviceDescriptionManager;
    private final ProvenanceManager provenanceManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final ModelManager modelManager;
    private final Property status = OWL.versionInfo;
    private final SearchIndexManager searchIndexManager;
    private final ObjectMapper objectMapper;

    @Autowired
    Models(AuthorizationManager authorizationManager,
           AuthenticatedUserProvider userProvider,
           EndpointServices endpointServices,
           GraphManager graphManager,
           JerseyResponseManager jerseyResponseManager,
           IDManager idManager,
           JerseyClient jerseyClient,
           ServiceDescriptionManager serviceDescriptionManager,
           ProvenanceManager provenanceManager,
           RHPOrganizationManager rhpOrganizationManager,
           ModelManager modelManager,
           SearchIndexManager searchIndexManager,
           ObjectMapper objectMapper) {

        this.searchIndexManager = searchIndexManager;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.endpointServices = endpointServices;
        this.graphManager = graphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.idManager = idManager;
        this.jerseyClient = jerseyClient;
        this.serviceDescriptionManager = serviceDescriptionManager;
        this.provenanceManager = provenanceManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.modelManager = modelManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get model from service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getModels(
        @Parameter(description = "Graph id") @QueryParam("id") String id,
        @Parameter(description = "Service category") @QueryParam("serviceCategory") String group,
        @Parameter(description = "prefix") @QueryParam("prefix") String prefix) {

        YtiUser user = userProvider.getUser();

        String queryString = QueryLibrary.fullModelQuery;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        if ((id == null || id.equals("undefined")) && (prefix != null && !prefix.equals("undefined"))) {
            logger.info("Resolving prefix: " + prefix);
            id = graphManager.getServiceGraphNameWithPrefix(prefix);
            if (id == null) {
                logger.warn("Invalid prefix: " + prefix);
                return jerseyResponseManager.invalidIRI();
            }
        }

        if ((group == null || group.equals("undefined")) && (id != null && !id.equals("undefined") && !id.equals("default"))) {
            logger.info("Model id:" + id);
            IRI modelIRI;

            try {
                modelIRI = idManager.constructIRI(id);
            } catch (IRIException e) {
                logger.warn("ID is invalid IRI!");
                return jerseyResponseManager.invalidIRI();
            }

            if (id.startsWith("urn:")) {
                return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
            }

            String sparqlService = endpointServices.getCoreSparqlAddress();
            String graphService = endpointServices.getCoreReadWriteAddress();

            /* TODO: Create Namespace service? */
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(graphService);
            Model model = accessor.getModel(id);

            if (model == null) {
                return jerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(model.getNsPrefixMap());

            pss.setIri("graph", modelIRI);

            pss.setCommandText(queryString);

            return jerseyClient.constructGraphFromService(pss.toString(), sparqlService);

        } else {

            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            queryString = QueryLibrary.fullModelsByGroupQuery;

            if (group != null && !group.equals("undefined")) {
                pss.setLiteral("groupCode", group);
            }

        }

        pss.setCommandText(queryString);

        Model modelList = graphManager.constructModelFromCoreGraph(pss.toString());

        if (!user.isSuperuser()) {
            ResIterator rem = modelList.listSubjectsWithProperty(status, "INCOMPLETE");
            while (rem.hasNext()) {
                Resource modelResource = rem.nextResource();
                if (user.isAnonymous()) {
                    modelList = modelList.remove(modelResource.listProperties());
                    // Seems to be faster than: modelList.removeAll(modelResource, null, (RDFNode) null);
                } else {
                    Set<UUID> orgUUIDs = user.getOrganizations(Role.ADMIN, Role.DATA_MODEL_EDITOR);
                    if (!orgUUIDs.contains(UUID.fromString(modelResource.getRequiredProperty(DCTerms.contributor).getResource().getURI().replace("urn:uuid:", "")))) {
                        modelList = modelList.remove(modelResource.listProperties());
                    }
                }
            }
        }

        return jerseyResponseManager.okModel(modelList);
        //return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());

    }

    /**
     * Replaces Graph in given service
     *
     * @returns empty Response
     */
    @POST
    @Operation(description = "Updates graph in service and writes service description to default")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Graph is saved"),
        @ApiResponse(responseCode = "400", description = "Invalid graph supplied"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "405", description = "Update not allowed"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Bad data?")
    })
    public Response postModel(
        @Parameter(description = "Updated model in application/ld+json", required = true)
            String body,
        @Parameter(description = "Model ID")
        @QueryParam("id") String graph) {

        try {

            YtiUser user = userProvider.getUser();

            Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

            if (parsedModel.size() == 0) {
                return jerseyResponseManager.notAcceptable();
            }

            DataModel newVocabulary = new DataModel(parsedModel, graphManager, rhpOrganizationManager);

            if (!newVocabulary.getIRI().toString().equals(graph)) {
                return jerseyResponseManager.invalidIRI();
            }

            logger.info("Getting old vocabulary:" + newVocabulary.getId());
            DataModel oldVocabulary = new DataModel(newVocabulary.getIRI(), graphManager);

            if (!authorizationManager.hasRightToEdit(newVocabulary) || !authorizationManager.hasRightToEdit(oldVocabulary)) {
                logger.info("User is not authorized");
                return jerseyResponseManager.unauthorized();
            }

            if(!oldVocabulary.getStatus().equals(newVocabulary.getStatus())) {
                newVocabulary.setStatusModified();
            }

            UUID provUUID = UUID.fromString(newVocabulary.getProvUUID().replaceFirst("urn:uuid:", ""));

            graphManager.updateModel(newVocabulary, oldVocabulary);

            searchIndexManager.updateIndexModel(newVocabulary);

            serviceDescriptionManager.createGraphDescription(newVocabulary.getId(), user.getId(), newVocabulary.getOrganizations());

            if (provenanceManager.getProvMode()) {
                provenanceManager.createProvEntityBundle(newVocabulary.getId(), newVocabulary.asGraph(), user.getId(), newVocabulary.getProvUUID(), null);
            }

            return jerseyResponseManager.successUrnUuid(provUUID);

        } catch (IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.error();
        } catch (RiotException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.notAcceptable();
        }

    }

    @PUT
    @Operation(description = "Create new model and update service description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "New model created"),
        @ApiResponse(responseCode = "200", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Overwrite is forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response putModel(
        @Parameter(description = "New graph in application/ld+json", required = true) String body) {

        try {

            Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

            if (parsedModel.size() == 0) {
                return jerseyResponseManager.notAcceptable();
            }

            DataModel newVocabulary = new DataModel(parsedModel, graphManager, rhpOrganizationManager);
            YtiUser user = userProvider.getUser();

            if (!authorizationManager.hasRightToEdit(newVocabulary)) {
                logger.info("User is not authorized");
                return jerseyResponseManager.unauthorized();
            }

            if (graphManager.isExistingGraph(newVocabulary.getId())) {
                return jerseyResponseManager.usedIRI(newVocabulary.getId());
            }

            String provUUID = newVocabulary.getProvUUID();

            if (provUUID == null) {
                return jerseyResponseManager.serverError();
            } else {
                logger.info("Storing new model: " + newVocabulary.getId());

                graphManager.createModel(newVocabulary);

                searchIndexManager.createIndexModel(newVocabulary);

                serviceDescriptionManager.createGraphDescription(newVocabulary.getId(), user.getId(), newVocabulary.getOrganizations());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvenanceActivityFromModel(newVocabulary.getId(), newVocabulary.asGraph(), newVocabulary.getProvUUID(), user.getId());
                }

                logger.info("Created new model: " + newVocabulary.getId());
                return jerseyResponseManager.successUrnUuid(provUUID, newVocabulary.getId());
            }

        } catch (IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.error();
        } catch (RiotException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.notAcceptable();
        }

    }

    @DELETE
    @Operation(description = "Delete graph from service and service description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Graph is deleted"),
        @ApiResponse(responseCode = "403", description = "Illegal graph parameter"),
        @ApiResponse(responseCode = "404", description = "No such graph"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "406", description = "Not acceptable")
    })
    public Response deleteModel(
        @Parameter(description = "Model ID", required = true)
        @QueryParam("id") String id) {

        /* Check that URIs are valid */
        IRI modelIRI;
        try {
            modelIRI = idManager.constructIRI(id);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException ex) {
            return jerseyResponseManager.invalidParameter();
        }

        YtiUser user = userProvider.getUser();

        if (!graphManager.isExistingGraph(modelIRI)) {
            return jerseyResponseManager.notFound();
        }

        if (!user.isSuperuser() && graphManager.modelStatusRestrictsRemoving(modelIRI)) {
            return jerseyResponseManager.cannotRemove();
        }

        DataModel deleteModel = new DataModel(modelIRI, graphManager);

        if (!authorizationManager.hasRightToEdit(deleteModel)) {
            return jerseyResponseManager.unauthorized();
        }

        searchIndexManager.removeModel(deleteModel.getId());

        if (provenanceManager.getProvMode()) {
            provenanceManager.invalidateModelProvenanceActivity(deleteModel.getId(), deleteModel.getProvUUID(), user.getId());
        }

        graphManager.deleteModel(deleteModel);

        return jerseyResponseManager.ok();
    }
}
