/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/organizations")
@Api(tags = { "Organizations" }, description = "Get organizations")
public class Organizations {

    private static final Logger logger = LoggerFactory.getLogger(Organizations.class.getName());
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;

    @Autowired
    Organizations(JerseyResponseManager jerseyResponseManager,
                  GraphManager graphManager) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
    }

    @GET
    @ApiOperation(value = "Get organizations", notes = "Get organizations from rhp")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "List of organizations"),
        @ApiResponse(code = 400, message = "Error from organization service"),
        @ApiResponse(code = 404, message = "Organization service not found") })
    @Produces("application/json")
    public Response getOrganizations() {
        Model orgModel = graphManager.getCoreGraph("urn:yti:organizations");
        if (orgModel != null && orgModel.size() > 1) {
            return jerseyResponseManager.okModel(orgModel);
        } else {
            return jerseyResponseManager.okEmptyContent();
        }
    }

}
