package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v2/user")
@Tag(name = "Users")
public class UserController {

    private final AuthenticatedUserProvider userProvider;

    UserController(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GetMapping
    @Operation(description = "Get authenticated user")
    @ApiResponse(responseCode = "200", description = "User object")
    public YtiUser getUser() {
        return userProvider.getUser();
    }
}