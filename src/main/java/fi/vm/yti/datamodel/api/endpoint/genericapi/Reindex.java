package fi.vm.yti.datamodel.api.endpoint.genericapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component
@Path("reindex")
@Api(tags = { "Admin" })
public class Reindex {

    private final SearchIndexManager searchIndexManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final AuthorizationManager authorizationManager;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Reindex(SearchIndexManager searchIndexManager,
            RHPOrganizationManager rhpOrganizationManager,
            AuthorizationManager authorizationManager,
            JerseyResponseManager jerseyResponseManager) {
        this.searchIndexManager = searchIndexManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.authorizationManager = authorizationManager;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Starts ES reindexing", notes = "Authentication required")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK")
    })
    public Response json() {
        if (!authorizationManager.hasRightToDropDatabase()) {
            return jerseyResponseManager.unauthorized();
        }

        searchIndexManager.reindex();

        return Response.ok().build();

    }
}
