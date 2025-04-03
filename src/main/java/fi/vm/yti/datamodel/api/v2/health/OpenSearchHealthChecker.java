package fi.vm.yti.datamodel.api.v2.health;

import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "health.opensearch.enabled", havingValue = "true", matchIfMissing = true)
public class OpenSearchHealthChecker implements HealthIndicator {
    private final OpenSearchClientWrapper openSearchClient;

    public OpenSearchHealthChecker(OpenSearchClientWrapper openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    @Override
    public Health health() {
        if (openSearchClient.isHealthy()) {
            return Health.up().withDetail("opensearch_connection", "OK").build();
        }
        return Health.down().withDetail("opensearch_connection", "NOT OK").build();
    }
}