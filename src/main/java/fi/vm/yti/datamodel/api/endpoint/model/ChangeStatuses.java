/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("v1/changeStatuses")
@Tag(name = "Model")
public class ChangeStatuses {


    private static final Logger logger = LoggerFactory.getLogger(ChangeStatuses.class);
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;

    @Autowired
    ChangeStatuses(AuthorizationManager authManager,
                   AuthenticatedUserProvider userProvider,
                   IDManager idManager,
                   JerseyResponseManager jerseyResponseManager,
                   GraphManager graphManager) {

        this.authorizationManager = authManager;
        this.userProvider = userProvider;
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
    }

    @PUT
    @Produces("application/ld+json")
    @Operation(description = "Change statuses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statuses were changed"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response changeStatuses(
        @Parameter(description = "Model ID") @QueryParam("model") String model,
        @Parameter(description = "Initial status") @QueryParam("initialStatus") String initialStatus,
        @Parameter(description = "End status") @QueryParam("endStatus") String endStatus) {

        IRI modelIRI = null;

        try {
            if (model != null && !model.equals("undefined")) modelIRI = idManager.constructIRI(model);
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        if(!graphManager.isExistingGraph(modelIRI)) {
            return jerseyResponseManager.invalidParameter();
        }

        YtiUser user = userProvider.getUser();
        DataModel dataModel = new DataModel(modelIRI,graphManager);

        if (!user.isSuperuser() && !authorizationManager.hasRightToEdit(dataModel)) {
            return jerseyResponseManager.unauthorized();
        }

        if(user.isSuperuser()) {
            graphManager.changeStatusesAsSuperUser(model, initialStatus, endStatus);
        } else {
            graphManager.changeStatuses(model, initialStatus, endStatus);
        }

        return jerseyResponseManager.ok();
    }
}
