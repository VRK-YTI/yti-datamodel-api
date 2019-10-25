package fi.vm.yti.datamodel.api.endpoint.concepts;

import com.jayway.jsonpath.JsonPath;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/conceptSuggestion")
@Tag(name = "Concept")
public class ConceptSuggestion {

    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final JerseyResponseManager jerseyResponseManager;
    private final ModelManager modelManager;
    private final IDManager idManager;
    private final JerseyClient jerseyClient;
    private final TerminologyManager terminologyManager;

    private static final Logger logger = LoggerFactory.getLogger(ConceptSuggestion.class.getName());

    @Autowired
    ConceptSuggestion(AuthorizationManager authorizationManager,
                      AuthenticatedUserProvider userProvider,
                      JerseyResponseManager jerseyResponseManager,
                      TerminologyManager terminologyManager,
                      ModelManager modelManager,
                      IDManager idManager,
                      JerseyClient jerseyClient) {

        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.jerseyResponseManager = jerseyResponseManager;
        this.terminologyManager = terminologyManager;
        this.modelManager = modelManager;
        this.idManager = idManager;
        this.jerseyClient = jerseyClient;
    }

    @PUT
    @Operation(description = "Create new concept suggestion")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "401", description = "User is not logged in"),
        @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response newConceptSuggestion(
        @Parameter(description = "Terminology uri", required = true) @QueryParam("terminologyUri") String terminologyUri,
        @Parameter(description = "Label", required = true) @QueryParam("label") String label,
        @Parameter(description = "Comment", required = true) @QueryParam("comment") String comment,
        @Parameter(description = "Initial language", required = true) @QueryParam("lang") String lang) {

        // TODO: schema = @Schema(allowableValues = "fi,en"

        if (!authorizationManager.hasRightToSuggestConcept()) {
            return jerseyResponseManager.unauthorized();
        }

        logger.info("Creating concept suggestion: " + label);

        String jsonString = terminologyManager.createConceptSuggestionJson(terminologyUri, lang, label, comment, userProvider.getUser().getId().toString());

        Response conceptResp = jerseyClient.saveConceptSuggestionUsingTerminologyAPI(jsonString, terminologyUri);

        if (conceptResp.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.info("Concept suggestion could not be saved! Invalid parameter or missing terminology?");
            return jerseyResponseManager.error();
        }

        String respString = conceptResp.readEntity(String.class);

        String newConceptUri = JsonPath.parse(respString).read("$.uri");
        return jerseyResponseManager.successUri(newConceptUri);

    }

}
