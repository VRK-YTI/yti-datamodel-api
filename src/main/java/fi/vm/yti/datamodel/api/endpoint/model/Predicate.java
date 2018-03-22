/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Path("predicate")
@Api(tags = {"Predicate"}, description = "Operations about reusable properties")
public class Predicate {

    private static final Logger logger = Logger.getLogger(Predicate.class.getName());
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;
    private final NamespaceManager namespaceManager;
    private final IDManager idManager;
    private final GraphManager graphManager;
    private final ProvenanceManager provenanceManager;
    private final ServiceDescriptionManager serviceDescriptionManager;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;

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
              ServiceDescriptionManager serviceDescriptionManager,
              JenaClient jenaClient,
              ModelManager modelManager) {

        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
        this.namespaceManager = namespaceManager;
        this.idManager = idManager;
        this.graphManager = graphManager;
        this.provenanceManager = provenanceManager;
        this.serviceDescriptionManager = serviceDescriptionManager;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get property from model", notes = "More notes about this method")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid model supplied"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @ApiParam(value = "Property id")
            @QueryParam("id") String id,
            @ApiParam(value = "Model id")
            @QueryParam("model") String model) {

        if(id==null || id.equals("undefined") || id.equals("default")) {

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            // TODO: Create namespacemap from models
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            String queryString = QueryLibrary.listPredicatesQuery;

            if(model!=null && !model.equals("undefined")) {
                pss.setIri("library", model);
                pss.setIri("hasPartGraph",model+"#HasPartGraph");
            }

            pss.setCommandText(queryString);

            return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());

        } else {

            if(idManager.isInvalid(id)) {
                return jerseyResponseManager.invalidIRI();
            }

            if(id.startsWith("urn:")) {
                return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */

            Map<String, String> namespaceMap = namespaceManager.getCoreNamespaceMap(id);

            if(namespaceMap==null) {
                logger.info("No model for "+id);
                return jerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.predicateQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            if(model!=null && !model.equals("undefined")) {
                pss.setIri("library", model);
            }

            return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());

        }

    }


    @POST
    @ApiOperation(value = "Create new property to certain model OR add reference from existing property to another model", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Graph is created"),
            @ApiResponse(code = 204, message = "Graph is saved"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 405, message = "Update not allowed"),
            @ApiResponse(code = 403, message = "Illegal graph parameter"),
            @ApiResponse(code = 400, message = "Invalid graph supplied"),
            @ApiResponse(code = 500, message = "Bad data?")
    })
    public Response postJson(
            @ApiParam(value = "New graph in application/ld+json", required = false)
                    String body,
            @ApiParam(value = "Property ID", required = true)
            @QueryParam("id")
                    String id,
            @ApiParam(value = "OLD Property ID")
            @QueryParam("oldid")
                    String oldid,
            @ApiParam(value = "Model ID", required = true)
            @QueryParam("model")
                    String model) {

        try {

            IRI modelIRI,idIRI,oldIdIRI = null;

            /* Check that URIs are valid */
            try {
                modelIRI = idManager.constructIRI(model);
                idIRI = idManager.constructIRI(id);
                /* If oldid exists */
                if(oldid!=null && !oldid.equals("undefined")) {
                    if(oldid.equals(id)) {
                        /* id and oldid cant be the same */
                        return jerseyResponseManager.usedIRI();
                    }
                    oldIdIRI = idManager.constructIRI(oldid);
                }
            }
            catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }

            String provUUID = null;

            if(isNotEmpty(body)) {

                YtiUser user = userProvider.getUser();
                ReusablePredicate updatePredicate = new ReusablePredicate(body, graphManager, serviceDescriptionManager, jenaClient, modelManager);

                if (!authorizationManager.hasRightToEdit(updatePredicate)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* Rename ID if oldIdIRI exists */
                if(oldIdIRI!=null) {
                    /* Prevent overwriting existing resources */
                    if(graphManager.isExistingGraph(idIRI)) {
                        logger.log(Level.WARNING, idIRI+" is existing graph!");
                        return jerseyResponseManager.usedIRI();
                    } else {
                        graphManager.updateResourceWithNewId(oldIdIRI,updatePredicate);
                        provUUID = updatePredicate.getProvUUID();
                        logger.info("Changed predicate id from:"+oldid+" to "+id);
                    }
                } else {
                    graphManager.updateResource(updatePredicate);
                    logger.info("Updated "+updatePredicate.getId());
                    provUUID = updatePredicate.getProvUUID();
                }

                if(provenanceManager.getProvMode()) {
                    provenanceManager.createProvEntityBundle(updatePredicate.getId(), updatePredicate.asGraph(), user.getEmail(), updatePredicate.getProvUUID(), oldIdIRI);
                }


            } else {

                if (!authorizationManager.hasRightToAddPredicateReference(modelIRI, id)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
                if(LDHelper.isResourceDefinedInNamespace(id, model)) {
                    // Selfreferences not allowed
                    return jerseyResponseManager.usedIRI();
                } else {
                    graphManager.insertExistingResourceToModel(id, model);
                    return jerseyResponseManager.ok();
                }
            }

            if(provUUID!=null) {
                return jerseyResponseManager.successUuid(provUUID);
            }
            else return jerseyResponseManager.notCreated();

        } catch(IllegalArgumentException ex) {
            logger.warning(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch(Exception ex) {
            Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }


    @PUT
    @ApiOperation(value = "Create new property to certain model", notes = "PUT Body should be json-ld")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Graph is created"),
            @ApiResponse(code = 204, message = "Graph is saved"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 405, message = "Update not allowed"),
            @ApiResponse(code = 403, message = "Illegal graph parameter"),
            @ApiResponse(code = 400, message = "Invalid graph supplied"),
            @ApiResponse(code = 500, message = "Bad data?")
    })
    public Response putJson(
            @ApiParam(value = "New graph in application/ld+json", required = true) String body) {

        try {

            ReusablePredicate newPredicate = new ReusablePredicate(body, graphManager, serviceDescriptionManager, jenaClient, modelManager);
            YtiUser user = userProvider.getUser();

            /* Prevent overwriting existing predicate */
            if(graphManager.isExistingGraph(newPredicate.getId())) {
                logger.log(Level.WARNING, newPredicate.getId()+" is existing predicate!");
                return jerseyResponseManager.usedIRI();
            }

            if(!authorizationManager.hasRightToEdit(newPredicate)) {
                return jerseyResponseManager.unauthorized();
            }

            String provUUID = newPredicate.getProvUUID();
            graphManager.createResource(newPredicate);

            if (provenanceManager.getProvMode()) {
                provenanceManager.createProvenanceActivityFromModel(newPredicate.getId(), newPredicate.asGraph(), newPredicate.getProvUUID(), user.getEmail());
            }

            if(provUUID!=null) {
                return jerseyResponseManager.successUrnUuid(provUUID,newPredicate.getId());
            }
            else {
                return jerseyResponseManager.notCreated();
            }

        } catch(IllegalArgumentException ex) {
            logger.warning(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch(Exception ex) {
            Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }


    @DELETE
    @ApiOperation(value = "Delete predicate graph or reference", notes = "Deletes predicate graph or reference")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Graph is deleted"),
            @ApiResponse(code = 403, message = "Illegal graph parameter"),
            @ApiResponse(code = 404, message = "No such graph"),
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response deletePredicate(
            @ApiParam(value = "Model ID", required = true)
            @QueryParam("model") String model,
            @ApiParam(value = "Predicate ID", required = true)
            @QueryParam("id") String id) {

        /* Check that URIs are valid */
        IRI modelIRI,idIRI;
        try {
            modelIRI = idManager.constructIRI(model);
            idIRI = idManager.constructIRI(id);
        }
        catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }


        /* If Predicate is defined in the model */
        if(id.startsWith(model)) {
            /* Remove graph */
            // Response resp = JerseyClient.deleteGraphFromService(id, services.getCoreReadWriteAddress());
            try {
                logger.info("Removeing "+idIRI.toString());
                ReusablePredicate deletePredicate = new ReusablePredicate(idIRI, graphManager, serviceDescriptionManager, jenaClient, modelManager);

                if (!authorizationManager.hasRightToEdit(deletePredicate)) {
                    return jerseyResponseManager.unauthorized();
                }

                graphManager.deleteResource(deletePredicate);

            } catch(IllegalArgumentException ex) {
                logger.warning(ex.toString());
                return jerseyResponseManager.unexpected();
            }
            return jerseyResponseManager.ok();
        } else {

            if (!authorizationManager.hasRightToRemovePredicateReference(modelIRI, idIRI)) {
                return jerseyResponseManager.unauthorized();
            }

            /* If removing referenced predicate */
            graphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);

            graphManager.deleteGraphReferenceFromExportModel(idIRI, modelIRI);
            // TODO: Not removed from export model
            return jerseyResponseManager.ok();
        }
    }
}
