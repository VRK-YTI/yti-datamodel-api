/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Path("class")
@Api(tags = {"Class"}, description = "Class operations")
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
    private final ServiceDescriptionManager serviceDescriptionManager;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;

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
    @ApiOperation(value = "Get class from model", notes = "Get class in JSON-LD")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such resource"),
            @ApiResponse(code = 400, message = "Invalid model supplied"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @ApiParam(value = "Class id")
            @QueryParam("id") String id,
            @ApiParam(value = "Model id")
            @QueryParam("model") String model) {

        if(id==null || id.equals("undefined") || id.equals("default")) {

            /* If no id is provided create a list of classes */
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            // TODO: Create namespacemap from all models
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            String queryString = QueryLibrary.listClassesQuery;

            if(model!=null && !model.equals("undefined")) {
                pss.setIri("library", model);
                pss.setIri("hasPartGraph",model+"#HasPartGraph");
            }

            pss.setCommandText(queryString);

            return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());

        } else {


            if(!idManager.isValidUrl(id)) {
                return jerseyResponseManager.invalidIRI();
            }

            if(id.startsWith("urn:")) {
                return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */

            Map<String, String> namespaceMap = namespaceManager.getCoreNamespaceMap(id);

            if(namespaceMap==null) {
                return jerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.classQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);


            if(model!=null && !model.equals("undefined")) {
                pss.setIri("library", model);
            }

            return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());
        }
    }

    @POST
    @ApiOperation(value = "Update class in certain model OR add reference from existing class to another model AND/OR change Class ID", notes = "PUT Body should be json-ld")
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
            @ApiParam(value = "New graph in application/ld+json", required = false) String body,
            @ApiParam(value = "Class ID", required = true)
            @QueryParam("id") String id,
            @ApiParam(value = "OLD Class ID")
            @QueryParam("oldid") String oldid,
            @ApiParam(value = "Model ID", required = true)
            @QueryParam("model") String model) {

        try {

            IRI modelIRI,idIRI,oldIdIRI = null;

            /* Check that URIs are valid */
            try {
                modelIRI = idManager.constructIRI(model);
                idIRI = idManager.constructIRI(id);
                /* If newid exists */
                if(oldid!=null && !oldid.equals("undefined")) {
                    if(oldid.equals(id)) {
                        /* id and newid cant be the same */
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

                Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

                if(parsedModel.size()==0) {
                    return jerseyResponseManager.notAcceptable();
                }

                ReusableClass updateClass = new ReusableClass(parsedModel, graphManager);
                YtiUser user = userProvider.getUser();

                if(!authorizationManager.hasRightToEdit(updateClass)) {
                    return jerseyResponseManager.unauthorized();
                }

                /* Rename ID if oldIdIRI exists */
                if(oldIdIRI!=null) {
                    /* Prevent overwriting existing resources */
                    if(graphManager.isExistingGraph(idIRI)) {
                        logger.warn( idIRI+" is existing graph!");
                        return jerseyResponseManager.usedIRI();
                    } else {
                        graphManager.updateResourceWithNewId(oldIdIRI,updateClass);
                        provUUID = updateClass.getProvUUID();
                        logger.info("Changed class id from:"+oldid+" to "+id);
                    }
                } else {
                    graphManager.updateResource(updateClass);
                    provUUID = updateClass.getProvUUID();
                }

                if(provenanceManager.getProvMode()) {
                    provenanceManager.createProvEntityBundle(updateClass.getId(), updateClass.asGraph(), user.getEmail(), updateClass.getProvUUID(), oldIdIRI);
                }


            } else {
                /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */

                if (!authorizationManager.hasRightToAddClassReference(modelIRI, id)) {
                    return jerseyResponseManager.unauthorized();
                }

                if(LDHelper.isResourceDefinedInNamespace(id, model)) {
                    // Self references not allowed
                    return jerseyResponseManager.usedIRI();
                } else {
                    graphManager.insertExistingResourceToModel(id, model);
                    // GraphManager.insertExistingGraphReferenceToModel(id, model);
                    // GraphManager.insertNewGraphReferenceToExportGraph(id, model);
                    // GraphManager.addCoreGraphToCoreGraph(id, model+"#ExportGraph");
                    logger.info("Created reference from "+model+" to "+id);
                    return jerseyResponseManager.ok();
                }
            }

            if(provUUID!=null) {
                return jerseyResponseManager.successUuid(provUUID);
            }
            else {
                return jerseyResponseManager.notCreated();
            }

        } catch(IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch(RiotException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.notAcceptable();
        } catch(Exception ex) {
            logger.warn( "Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }

    @PUT
    @ApiOperation(value = "Create new class to certain model", notes = "PUT Body should be json-ld")
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

            Model parsedModel = modelManager.createJenaModelFromJSONLDString(body);

            if(parsedModel.size()==0) {
                return jerseyResponseManager.notAcceptable();
            }

            ReusableClass newClass = new ReusableClass(parsedModel, graphManager);
            YtiUser user = userProvider.getUser();

            if (!authorizationManager.hasRightToEdit(newClass)) {
                return jerseyResponseManager.unauthorized();
            }

            /* Prevent overwriting existing classes */
            if(graphManager.isExistingGraph(newClass.getId())) {
                logger.warn( newClass.getId()+" is existing class!");
                return jerseyResponseManager.usedIRI();
            }

            String provUUID = newClass.getProvUUID();

            if (provUUID == null) {
                return jerseyResponseManager.serverError();
            }
            else {
                // newClass.create();
                graphManager.createResource(newClass);
                logger.info("Created "+newClass.getId());

                if (provenanceManager.getProvMode()) {
                    provenanceManager.createProvenanceActivityFromModel(newClass.getId(), newClass.asGraph(), newClass.getProvUUID(), user.getEmail());
                }

                return jerseyResponseManager.successUrnUuid(newClass.getProvUUID(), newClass.getId());
            }

        } catch(IllegalArgumentException ex) {
            logger.warn(ex.toString());
            return jerseyResponseManager.invalidParameter();
        } catch(Exception ex) {
            logger.warn( "Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }

    @DELETE
    @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Graph is deleted"),
            @ApiResponse(code = 403, message = "Illegal graph parameter"),
            @ApiResponse(code = 404, message = "No such graph"),
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response deleteClass(
            @ApiParam(value = "Model ID", required = true)
            @QueryParam("model") String model,
            @ApiParam(value = "Class ID", required = true)
            @QueryParam("id") String id) {

        /* Check that URIs are valid */
        IRI modelIRI,idIRI;
        try {
            modelIRI = idManager.constructIRI(model);
            idIRI = idManager.constructIRI(id);
        }
        catch(NullPointerException e) {
            return jerseyResponseManager.invalidIRI();
        }
        catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }


        /* If Class is defined in the model */
        if(id.startsWith(model)) {
            /* Remove graph */

            try {
                ReusableClass deleteClass = new ReusableClass(idIRI, graphManager);

                if (!authorizationManager.hasRightToEdit(deleteClass)) {
                    return jerseyResponseManager.unauthorized();
                }

               graphManager.deleteResource(deleteClass);

            } catch(IllegalArgumentException ex) {
                logger.warn(ex.toString());
                return jerseyResponseManager.unexpected();
            }

            return jerseyResponseManager.ok();
        } else {

            if (!authorizationManager.hasRightToRemoveClassReference(modelIRI, idIRI)) {
                return jerseyResponseManager.unauthorized();
            }

            /* If removing referenced class */
            graphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);
            graphManager.deleteGraphReferenceFromExportModel(idIRI, modelIRI);
            return jerseyResponseManager.ok();
        }
    }

}
