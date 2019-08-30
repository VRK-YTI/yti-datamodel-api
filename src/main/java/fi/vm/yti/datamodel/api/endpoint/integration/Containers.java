package fi.vm.yti.datamodel.api.endpoint.integration;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import io.swagger.annotations.*;

import org.apache.jena.query.ParameterizedSparqlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Component
@Path("v1/integration/containers")
@Api(tags = { "Integration" }, description = "Operations about models")
public class Containers {

    private static final Logger logger = LoggerFactory.getLogger(Containers.class.getName());
    private final EndpointServices endpointServices;
    private final JenaClient jenaClient;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Containers(
        EndpointServices endpointServices,
        JenaClient jenaClient,
        JerseyResponseManager jerseyResponseManager) {

        this.endpointServices = endpointServices;
        this.jenaClient = jenaClient;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Get containers from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
        @ApiParam(value = "Language")
        @QueryParam("language") String lang,
        @ApiParam(value = "Status")
        @QueryParam("status") String status) {

        if (status == null || status.length() < 4) {
            status = "DRAFT,INVALID,RETIRED,SUBMITTED,SUPERSEDED,VALID";
        }

        String statusList = "";
        String[] statuses = status.split(",");
        for (int i = 0; i < statuses.length; i++) {
            statusList += "'" + statuses[i].trim() + "' ";
        }

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        if (lang != null && lang.length() > 1) {
            pss.setCommandText(QueryLibrary.listContainersByPreflabelQuery.replace("?statusList", statusList));
            pss.setLiteral("language", lang);
        } else {
            pss.setCommandText(QueryLibrary.listContainersByModifiedQuery.replace("?statusList", statusList));
        }

        return jerseyResponseManager.ok(jenaClient.selectJson(endpointServices.getCoreSparqlAddress(), pss.asQuery()), "application/json");

    }

}
