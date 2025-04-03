package fi.vm.yti.datamodel.api.v2.health;

import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "health.fuseki.enabled", havingValue = "true", matchIfMissing = true)
public class FusekiHealthChecker implements HealthIndicator {
    private final CoreRepository repository;

    public FusekiHealthChecker(CoreRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            return repository.isHealthy() ?
                    Health.up().withDetail("fuseki_connection", "OK").build() :
                    Health.down().withDetail("fuseki_connection", "NOT OK").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("fuseki_connection", "NOT OK")
                    .withDetail("exception", e.getMessage())
                    .build();
        }
    }
}
