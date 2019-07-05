package fi.vm.yti.datamodel.api.endpoint.search;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.ModelSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ModelSearchResponse;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component
@Path("/searchModels")
@Api(tags = { "Index" })
public class ModelSearch {

    private static final Logger logger = LoggerFactory.getLogger(ModelSearch.class.getName());
    private AuthenticatedUserProvider userProvider;
    private SearchIndexManager searchIndexManager;
    private JerseyResponseManager jerseyResponseManager;
    private ObjectMapper objectMapper;

    @Autowired
    public ModelSearch(AuthenticatedUserProvider userProvider,
                       SearchIndexManager searchIndexManager,
                       JerseyResponseManager jerseyResponseManager,
                       ObjectMapper objectMapper) {
        this.userProvider = userProvider;
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Invalid JSON!")
    })
    public Response searchModels(ModelSearchRequest request) {
        YtiUser user = userProvider.getUser();
        ModelSearchResponse response = searchIndexManager.searchModels(request, user.getOrganizations(Role.ADMIN, Role.DATA_MODEL_EDITOR));
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

}
