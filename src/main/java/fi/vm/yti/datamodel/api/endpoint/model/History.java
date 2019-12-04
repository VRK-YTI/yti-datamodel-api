/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/history")
@Tag(name = "History" )
public class History {

    private static final Logger logger = LoggerFactory.getLogger(History.class.getName());

    private final NamespaceManager namespaceManager;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;
    private final JerseyResponseManager jerseyResponseManager;
    private final IDManager idManager;
    private final ModelManager modelManager;
    private final JenaClient jenaClient;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    History(NamespaceManager namespaceManager,
            EndpointServices endpointServices,
            JerseyClient jerseyClient,
            JerseyResponseManager jerseyResponseManager,
            IDManager idManager,
            ModelManager modelManager,
            JenaClient jenaClient,
            AuthenticatedUserProvider userProvider) {

        this.namespaceManager = namespaceManager;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
        this.idManager = idManager;
        this.modelManager = modelManager;
        this.jenaClient = jenaClient;
        this.userProvider = userProvider;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get activity history for the resource")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getHistory(
        @Parameter(description = "resource id") @QueryParam("id") String id,
        @Parameter(description = "Peek", schema = @Schema(defaultValue = "false")) @QueryParam("peek") boolean peek) {

        // TODO: Remove or refactor history

        if (id == null || id.equals("undefined") || id.equals("default") || peek) {

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            Map<String, String> namespacemap = namespaceManager.getCoreNamespaceMap();
            namespacemap.putAll(LDHelper.PREFIX_MAP);

            pss.setNsPrefixes(namespacemap);

            String queryString = "CONSTRUCT { "
                + "?activity a prov:Activity . "
                + "?activity prov:wasAttributedTo ?user . "
                + "?activity dcterms:modified ?modified . "
                + "?activity dcterms:identifier ?entity . "
                + " } "
                + "WHERE {"
                + "GRAPH ?activity {"
                + "?activity a prov:Activity . "
                + "?activity prov:used ?entity . "
                + "?entity a prov:Entity . "
                + "?entity prov:wasAttributedTo ?user . "
                + "?entity prov:generatedAtTime ?modified . "
                + "}} ORDER BY DESC(?modified)";

            pss.setCommandText(queryString);

            if (id != null && peek) {
                pss.setIri("activity", id);
            }

            return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getProvReadSparqlAddress());

        } else {
            logger.info("Gettin " + id + " from prov");
            return jerseyClient.getGraphResponseFromService(id, endpointServices.getProvReadWriteAddress());
        }
    }

    @PUT
    @Consumes("application/json+ld")
    @Operation(description="Update history graph")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response messWithHistory(
        @Parameter(description = "New graph in application/ld+json", required = true) String body,
        @Parameter(description = "resource id") @QueryParam("id") String id) {
        YtiUser user = userProvider.getUser();

        if(!user.isSuperuser()) {
            return jerseyResponseManager.unauthorized();
        }

        if(id!=null && !id.isEmpty()) {
            IRI historyIRI;

            try {
                historyIRI = idManager.constructIRI(id);
            } catch (IRIException e) {
                logger.warn("GRAPH ID is invalid IRI!");
                return jerseyResponseManager.invalidIRI();
            }

            Model newHistory = modelManager.createJenaModelFromJSONLDString(body);

            if (newHistory.size() < 5) {
                return jerseyResponseManager.invalidParameter();
            }

            jenaClient.putModelToProv(id,newHistory);

            return jerseyResponseManager.ok();
        }
        else {
            return jerseyResponseManager.invalidParameter();
        }
    }
}
