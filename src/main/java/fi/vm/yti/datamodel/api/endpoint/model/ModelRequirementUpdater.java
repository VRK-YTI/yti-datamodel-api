/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("v1/newModelRequirement")
@Tag(name = "Model" )
public class ModelRequirementUpdater {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    ModelRequirementUpdater(AuthorizationManager authorizationManager,
                            AuthenticatedUserProvider userProvider,
                            IDManager idManager,
                            JerseyResponseManager jerseyResponseManager,
                            GraphManager graphManager) {
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
    }

    @PUT
    @Operation(description = "Add resource model to required models")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Resource model is added"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to add resource") })
    public Response putNewRequirement(
        @Parameter(description = "Model URI", required = true) @QueryParam("model") String model,
        @Parameter(description = "Resource URI", required = true) @QueryParam("resource") String resource) {

        IRI modelIRI, resourceIRI;

        if(model == null || resource== null || model.isEmpty() || resource.isEmpty()) {
            jerseyResponseManager.invalidIRI();
        }

        if(model.endsWith("#")) {
            if(resource.startsWith(model)) {
                jerseyResponseManager.invalidIRI();
            }
            model = model.substring(0, model.length() - 1);
        } else {
            if(resource.startsWith(model+"#")) {
                jerseyResponseManager.invalidIRI();
            }
        }

        try {
            modelIRI = idManager.constructIRI(model);
            resourceIRI = idManager.constructIRI(resource);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        DataModel dm = new DataModel(modelIRI,graphManager);

        if(userProvider.getUser().isSuperuser() || authorizationManager.hasRightToEdit(dm)) {
            graphManager.addResourceNamespaceToModel(modelIRI, resourceIRI);
            return jerseyResponseManager.ok();
        } else {
            return jerseyResponseManager.unauthorized();
        }

    }

}
