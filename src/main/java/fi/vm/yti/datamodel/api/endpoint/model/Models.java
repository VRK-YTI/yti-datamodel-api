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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component
@Path("v1/model")
@Api(tags = { "Model" }, description = "Operations about models")
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
    @ApiOperation(value = "Get model from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "Graph id")
        @QueryParam("id") String id,
        @ApiParam(value = "Service category")
        @QueryParam("serviceCategory") String group,
        @ApiParam(value = "prefix")
        @QueryParam("prefix") String prefix) {

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
    @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Graph is saved"),
        @ApiResponse(code = 400, message = "Invalid graph supplied"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 405, message = "Update not allowed"),
        @ApiResponse(code = 403, message = "Illegal graph parameter"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Bad data?")
    })
    public Response postJson(
        @ApiParam(value = "Updated model in application/ld+json", required = true)
            String body,
        @ApiParam(value = "Model ID")
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

            UUID provUUID = UUID.fromString(newVocabulary.getProvUUID().replaceFirst("urn:uuid:", ""));

            graphManager.updateModel(newVocabulary);

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
    @ApiOperation(value = "Create new model and update service description", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "New model created"),
        @ApiResponse(code = 200, message = "Bad request"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Overwrite is forbidden"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response putJson(
        @ApiParam(value = "New graph in application/ld+json", required = true) String body) {

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
    @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Graph is deleted"),
        @ApiResponse(code = 403, message = "Illegal graph parameter"),
        @ApiResponse(code = 404, message = "No such graph"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 406, message = "Not acceptable")
    })
    public Response deleteModel(
        @ApiParam(value = "Model ID", required = true)
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

        if (!graphManager.isExistingGraph(modelIRI)) {
            return jerseyResponseManager.notFound();
        }

        if (graphManager.modelStatusRestrictsRemoving(modelIRI)) {
            return jerseyResponseManager.cannotRemove();
        }

        DataModel deleteModel = new DataModel(modelIRI, graphManager);

        if (!authorizationManager.hasRightToEdit(deleteModel)) {
            return jerseyResponseManager.unauthorized();
        }

        searchIndexManager.removeModel(deleteModel.getId());

        graphManager.deleteModel(deleteModel);

        return jerseyResponseManager.ok();
    }
}
