package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.security.AuthorizationManagerImpl;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.migration.Migration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Component
@Path("v1/reset")
@Tag(name = "Admin")
public class Reset {

    private final GraphManager graphManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final NamespaceManager namespaceManager;
    private final AuthorizationManagerImpl authorizationManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final Migration migrationManager;

    Reset(GraphManager graphManager,
          RHPOrganizationManager rhpOrganizationManager,
          NamespaceManager namespaceManager,
          AuthorizationManagerImpl authorizationManager,
          JerseyResponseManager jerseyResponseManager,
          Migration migrationManager) {

        this.graphManager = graphManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.namespaceManager = namespaceManager;
        this.authorizationManager = authorizationManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.migrationManager = migrationManager;
    }

    @GET
    @Operation(description = "Drops everything")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Response drop() {

        if (!authorizationManager.hasRightToDropDatabase()) {
            return jerseyResponseManager.unauthorized();
        }

        graphManager.deleteGraphs();
        migrationManager.migrate();
        rhpOrganizationManager.initOrganizationsFromRHP();
        namespaceManager.addDefaultNamespacesToCore();

        return Response.ok().build();
    }
}
