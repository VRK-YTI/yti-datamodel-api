package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.SubscriptionDTO;
import fi.vm.yti.datamodel.api.v2.service.DataModelSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v2/subscribe")
@Tag(name = "Subscribe" )
public class SubscribeController {

    private final DataModelSubscriptionService service;

    public SubscribeController(DataModelSubscriptionService service) {
        this.service = service;
    }

    @Operation(summary = "Subscribe to model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription created"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PostMapping("/{prefix}")
    public ResponseEntity<String> subscribe(@PathVariable String prefix) {
        var subscriptionArn = service.subscribe(prefix);
        return ResponseEntity.ok(subscriptionArn);
    }

    @Operation(summary = "List user's subscriptions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions fetched successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping
    public List<SubscriptionDTO> subscriptions() {
        return service.listSubscriptions();
    }

    @Operation(summary = "Get user's subscription for model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription fetched"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/{prefix}")
    public String getSubscription(@PathVariable String prefix) {
        return service.getSubscription(prefix);
    }

    @Operation(summary = "Unsubscribe from model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Unsubscribed successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(@RequestParam String subscriptionArn) {
        service.unsubscribe(subscriptionArn);
        return ResponseEntity.noContent().build();
    }
}
