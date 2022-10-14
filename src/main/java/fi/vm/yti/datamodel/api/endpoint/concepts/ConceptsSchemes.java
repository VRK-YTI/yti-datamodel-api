/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.TerminologyManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/conceptSchemes")
@Tag(name = "Concept")
public class ConceptsSchemes {

    private final JerseyResponseManager jerseyResponseManager;
    private final TerminologyManager terminologyManager;
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    ConceptsSchemes(AuthorizationManager authorizationManager,
                    AuthenticatedUserProvider userProvider,
                    JerseyResponseManager jerseyResponseManager,
                    TerminologyManager terminologyManager) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.terminologyManager = terminologyManager;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Lists terminologies from Terminology API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Concepts"),
        @ApiResponse(responseCode = "406", description = "Term not defined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response conceptSchemes() {

        YtiUser user = userProvider.getUser();

        if(user.isSuperuser()) {
            return jerseyResponseManager.okModel(terminologyManager.getSchemesModelFromTerminologyAPI(null, true));
        } else if(user.isAnonymous()) {
            return jerseyResponseManager.okModel(terminologyManager.getSchemesModelFromTerminologyAPI(null, false));
        } else {
            final Map<UUID, Set<Role>> rolesInOrganizations = user.getRolesInOrganizations();
            Set<String> orgIds = rolesInOrganizations.keySet().stream().map(UUID::toString).collect(Collectors.toSet());
            if(orgIds.isEmpty()) {
                return jerseyResponseManager.okModel(terminologyManager.getSchemesModelFromTerminologyAPI(null, false));
            } else {
                return jerseyResponseManager.okModel(terminologyManager.getSchemesModelFromTerminologyAPI(null, false, orgIds));
            }
        }
    }
}
