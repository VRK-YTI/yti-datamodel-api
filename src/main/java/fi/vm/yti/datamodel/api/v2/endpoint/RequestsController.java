package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.GroupManagementUserRequestDTO;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/requests")
@Tag(name = "Request" )
public class RequestsController {

    private final GroupManagementService groupManagementService;

    public RequestsController(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    @Operation(summary = "Get list of authorization requests for the current user", description = "Get the currently authenticated user's pending requests for roles for organizations")
    @ApiResponse(responseCode = "200", description = "The currently authenticated user's pending requests for roles for organizations")
    @ApiResponse(responseCode = "401", description = "If the caller is not not authenticated user")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUserRequestDTO> getUserRequests() {
        return groupManagementService.getUserRequests();
    }

    @Operation(summary = "Request authorization for a organization", description = "Request to be added to an organization in TERMINOLOGY_EDITOR role")
    @ApiResponse(responseCode = "200", description = "Request submitted successfully")
    @ApiResponse(responseCode = "401", description = "If the caller is not not authenticated user")
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    void sendRequest(
            @Parameter(description = "UUID for the organization") @RequestParam UUID organizationId,
            @Parameter(description = "Comma separated list of roles for organisation") @RequestParam(required = false) String[] roles) {
        groupManagementService.sendRequest(organizationId, roles);
    }
}
