/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.*;

import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/history")
@Api(tags = { "History" }, description = "Get list of revisions of the resource from change history")
public class History {

    private static final Logger logger = LoggerFactory.getLogger(History.class.getName());

    private final NamespaceManager namespaceManager;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;

    @Autowired
    History(NamespaceManager namespaceManager,
            EndpointServices endpointServices,
            JerseyClient jerseyClient) {

        this.namespaceManager = namespaceManager;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get activity history for the resource", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "resource id")
        @QueryParam("id") String id,
        @ApiParam(value = "Peek", defaultValue = "false")
        @QueryParam("peek") boolean peek) {

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
                + "?activity a prov:Activity . "
                + "?activity prov:used ?entity . "
                + "?entity a prov:Entity . "
                + "?entity prov:wasAttributedTo ?user . "
                + "?entity prov:generatedAtTime ?modified . "
                + "} ORDER BY DESC(?modified)";

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
}
