/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.DCTerms;
import org.glassfish.jersey.uri.UriComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("replicate")
@Api(tags = { "Admin" }, description = "Returns information about replicable models")
public class Replicator {

    private static final Logger logger = LoggerFactory.getLogger(Replicator.class.getName());

    private final AuthorizationManager authorizationManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final JenaClient jenaClient;
    private final GraphManager graphManager;
    private final EndpointServices endpointServices;
    private final ProvenanceManager provenanceManager;

    @Autowired
    Replicator(AuthorizationManager authorizationManager,
               JerseyResponseManager jerseyResponseManager,
               JerseyClient jerseyClient,
               JenaClient jenaClient,
               GraphManager graphManager,
               EndpointServices endpointServices,
               ProvenanceManager provenanceManager) {
        this.authorizationManager = authorizationManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.jenaClient = jenaClient;
        this.graphManager = graphManager;
        this.endpointServices = endpointServices;
        this.provenanceManager = provenanceManager;
    }

    @GET
    @ApiOperation(value = "OK to replicate?")
    @Produces("application/json")
    public boolean getStatus() {
        /* Add proper logic */
        return true;
    }

    /**
     * Replaces Graph in given service
     *
     * @returns empty Response
     */
    @PUT
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
        @ApiParam(value = "IOW Service ID in form of http://domain/api/rest/ ", required = true)
        @QueryParam("service") String service) {

        if (service == null || service.equals("undefined")) {
            return jerseyResponseManager.invalidIRI();
        }

        if (!authorizationManager.canReplicate()) {
            return jerseyResponseManager.unauthorized();
        }

        Boolean replicate = jerseyClient.readBooleanFromURL(service + "replicate");

        if (replicate != null && replicate.booleanValue()) {
            logger.info("Replicating data from " + service);

            logger.warn("Warning! Deleting graphs!");
            graphManager.deleteGraphs();

            // <urn:csc:iow:namespaces>
            String namespaces = service + "exportResource?graph=" + LDHelper.encode("urn:csc:iow:namespaces");
            Model namespaceModel = jerseyClient.getResourceAsJenaModel(namespaces);
            logger.info("Copying namespaces. Model size: " + namespaceModel.size());
            jenaClient.putModelToCore("urn:csc:iow:namespaces", namespaceModel);

            // <urn:yti:servicecategories>

            String serviceCategories = service + "exportResource?graph=" + LDHelper.encode("urn:yti:servicecategories");
            Model serviceModel = jerseyClient.getResourceAsJenaModel(serviceCategories);
            logger.info("Copying service categories: " + serviceModel.size());
            jenaClient.putModelToCore("urn:yti:servicecategories", serviceModel);

            // <urn:yti:organizations>

            String organizations = service + "exportResource?graph=" + LDHelper.encode("urn:yti:organizations");
            Model orgModel = jerseyClient.getResourceAsJenaModel(organizations);
            logger.info("Copying organizations: " + orgModel.size());
            jenaClient.putModelToCore("urn:yti:organizations", orgModel);

        }

        try {

            // Copy service description & Models

            String SD = "http://www.w3.org/ns/sparql-service-description#";
            Model SDModel = jerseyClient.getResourceAsJenaModel(service + "serviceDescription");

            if (SDModel != null && SDModel.size() > 0) {

                // <urn:csc:iow:sd>
                logger.info("Copying service description: " + SDModel.size());
                jenaClient.putModelToCore("urn:csc:iow:sd", SDModel);

                NodeIterator rit = SDModel.listObjectsOfProperty(ResourceFactory.createProperty(SD, "name"));
                if (rit.hasNext()) {

                    while (rit.hasNext()) {
                        Resource res = rit.next().asResource();
                        logger.info("SD graph name: " + res.toString());
                        replicateServices(service, res.toString());
                    }

                } else {
                    return jerseyResponseManager.error();
                }

            } else {
                return jerseyResponseManager.error();
            }
            return jerseyResponseManager.okEmptyContent();

        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            return jerseyResponseManager.error();
        }

    }

    public void replicateServices(String service,
                                  String model) {

        Model exportedModel = jerseyClient.getResourceAsJenaModel(service + "exportResource?graph=" + UriComponent.encode(model, UriComponent.Type.QUERY_PARAM));
        String prefix = exportedModel.listStatements(ResourceFactory.createResource(model), LDHelper.curieToProperty("dcap:preferredXMLNamespacePrefix"), (Literal) null).nextStatement().getString();

        logger.info("Model: " + model + " Model prefix: " + prefix);
        logger.info("---------------------------------------------------------");

        // Store model
        jenaClient.putModelToCore(model, exportedModel);

        // Model resource history
        String modelHistoryURL = service + "history?id=" + UriComponent.encode(model, UriComponent.Type.QUERY_PARAM);
        Model modelHistoryModel = jerseyClient.getResourceAsJenaModel(modelHistoryURL);
        provenanceManager.putToProvenanceGraph(modelHistoryModel, model);
        ResIterator modelProvIter = modelHistoryModel.listSubjectsWithProperty(ProvenanceManager.generatedAtTime);

        while (modelProvIter.hasNext()) {
            String provModelURI = modelProvIter.next().asResource().toString();
            Model provModelRes = jerseyClient.getResourceAsJenaModel(service + "history?id=" + UriComponent.encode(provModelURI, UriComponent.Type.QUERY_PARAM));
            provenanceManager.putToProvenanceGraph(provModelRes, provModelURI);
        }

        // HasPartGraph
        String uri = service + "exportResource?graph=" + UriComponent.encode(model + "#HasPartGraph", UriComponent.Type.QUERY_PARAM);
        logger.info("Getting HasPartGraph: " + uri);
        Model hasPartModel = jerseyClient.getResourceAsJenaModel(uri);

        // Creating new HasPartGraph
        jenaClient.putModelToCore(model + "#HasPartGraph", hasPartModel);

        // ExportGraph
        String euri = service + "exportResource?graph=" + UriComponent.encode(model + "#ExportGraph", UriComponent.Type.QUERY_PARAM);
        logger.info("ExportGraph:" + euri);
        Model exportModel = jerseyClient.getResourceAsJenaModel(euri);
        jenaClient.putModelToCore(model + "#ExportGraph", exportModel);

        // PositionGraph
        String puri = service + "exportResource?graph=" + UriComponent.encode(model + "#PositionGraph", UriComponent.Type.QUERY_PARAM);

        Model positionModel = jerseyClient.getResourceAsJenaModel(puri);

        if (positionModel.size() > 1) {
            jenaClient.putModelToCore(model + "#PositionGraph", positionModel);
        }

        // Resources

        NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);

        while (nodIter.hasNext()) {
            Resource part = nodIter.nextNode().asResource();
            String resourceName = part.toString();

            if (resourceName.startsWith(model)) {
                logger.info("Replicating resource: " + resourceName);
                String resourceURI = service + "exportResource?graph=" + UriComponent.encode(resourceName, UriComponent.Type.QUERY_PARAM);
                Model resourceModel = jerseyClient.getResourceAsJenaModel(resourceURI);
                jenaClient.putModelToCore(resourceName, resourceModel);

                String historyURL = service + "history?id=" + UriComponent.encode(resourceName, UriComponent.Type.QUERY_PARAM);
                Dataset provResourceDataset = DatasetFactory.create();
                Model resourceHistoryModel = jerseyClient.getResourceAsJenaModel(historyURL);
                provenanceManager.putToProvenanceGraph(resourceHistoryModel, resourceName);

                ResIterator resProvIter = resourceHistoryModel.listSubjectsWithProperty(ProvenanceManager.generatedAtTime);

                while (resProvIter.hasNext()) {
                    String provResURI = resProvIter.next().asResource().toString();
                    Model provRes = jerseyClient.getResourceAsJenaModel(service + "history?id=" + UriComponent.encode(provResURI, UriComponent.Type.QUERY_PARAM));
                    provenanceManager.putToProvenanceGraph(provRes, provResURI);
                }

            } else {
                logger.info("Reference to external resource " + resourceName);
            }
        }

        logger.info("---------------------------------------------------------");

    }

    //FIXME: Using RDFCOnnection would be preferred if namespaces would preserve in TRIG or JSON-LD Format.
    @Deprecated
    public void replicateServices(String externalService) {

        graphManager.deleteGraphs();

        try (RDFConnection conn = RDFConnectionFactory.connect(endpointServices.getEndpoint() + "/core")) {
            Txn.executeWrite(conn, () -> {
                Dataset externalDataset = jerseyClient.getExternalJSONLDDatasets(externalService + "exportGraphs?service=core&content-type=application%2Fld%2Bjson");
                logger.info("Size of the CORE dataset: " + externalDataset.asDatasetGraph().size());
                conn.putDataset(externalDataset);
            });
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
        }

        try (RDFConnection conn = RDFConnectionFactory.connect(endpointServices.getEndpoint() + "/prov")) {
            Txn.executeWrite(conn, () -> {
                Dataset externalDataset = jerseyClient.getExternalJSONLDDatasets(externalService + "exportGraphs?service=prov&content-type=application%2Fld%2Bjson");
                logger.info("Size of the PROV dataset: " + externalDataset.asDatasetGraph().size());
                conn.putDataset(externalDataset);
            });
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
        }

    }
}
