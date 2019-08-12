/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import javax.ws.rs.GET;
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
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component
@Path("changeStatuses")
@Api(tags = { "Model" })
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

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Change statuses")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Statuses were changed"),
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
        @ApiResponse(code = 404, message = "Service not found") })
    public Response ChangeStatuses(
        @ApiParam(value = "Model ID") @QueryParam("model") String model,
        @ApiParam(value = "Initial status") @QueryParam("initialStatus") String initialStatus,
        @ApiParam(value = "End status") @QueryParam("endStatus") String endStatus) {

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
