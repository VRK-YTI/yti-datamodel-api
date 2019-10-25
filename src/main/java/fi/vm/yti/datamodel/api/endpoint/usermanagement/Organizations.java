/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Organizations")
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
    @Operation(description = "Get organizations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "List of organizations"),
        @ApiResponse(responseCode = "400", description = "Error from organization service"),
        @ApiResponse(responseCode = "404", description = "Organization service not found") })
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
