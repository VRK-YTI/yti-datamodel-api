package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/freePrefix")
@Tag(name = "Model")
public class FreePrefix {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final ApplicationProperties applicationProperties;
    private static final Logger logger = LoggerFactory.getLogger(FreePrefix.class.getName());

    @Autowired
    FreePrefix(IDManager idManager,
           JerseyResponseManager jerseyResponseManager,
           GraphManager graphManager,
           ApplicationProperties applicationProperties) {
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
        this.applicationProperties = applicationProperties;
    }

    @GET
    @Produces("application/json")
    @Operation(description = "Returns true if Prefix is valid and not in use")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "False or True response")
    })
    public Response getFreeId(@Parameter(description = "Prefix", required = true) @QueryParam("prefix") String prefix) {

        if (!LDHelper.isValidPrefix(prefix) || LDHelper.isReservedPrefix(prefix) || LDHelper.isReservedWord(prefix)) {
            return jerseyResponseManager.sendBoolean(false);
        }

        String namespace = applicationProperties.getDefaultNamespace()+prefix;

        if (idManager.isInvalid(namespace)) {
            return jerseyResponseManager.sendBoolean(false);
        }

        return jerseyResponseManager.sendBoolean(!graphManager.isExistingGraph(namespace));
    }
}
