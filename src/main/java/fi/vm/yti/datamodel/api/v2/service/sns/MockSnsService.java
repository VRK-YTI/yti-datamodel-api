package fi.vm.yti.datamodel.api.v2.service.sns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile({"junit", "local", "docker"})
public class MockSnsService implements SnsService {

    private static final Logger LOG = LoggerFactory.getLogger(MockSnsService.class);

    private static final Map<String, String> subscriptions = new HashMap<>();

    @Override
    public String subscribe(String prefix, String email) {
        LOG.info("subscribe to {}", prefix);
        var subsId = UUID.randomUUID().toString();
        subscriptions.put(prefix, subsId);
        return subsId;
    }

    @Override
    public void unsubscribe(String subscriptionARN) {
        LOG.info("unsubscribe {}", subscriptionARN);
        var sub = subscriptions.entrySet().stream().filter(e -> e.getValue().equals(subscriptionARN)).findFirst();
        sub.ifPresent(stringStringEntry -> subscriptions.remove(stringStringEntry.getKey()));
    }

    @Override
    public void publish(String prefix, String title, String message) {
        LOG.info("publish notification for {}, title {}", prefix, title);
    }

    @Override
    public List<Subscription> listSubscriptions(String email) {
        return List.of();
    }

    @Override
    public String getSubscription(String prefix, String email) {
        return subscriptions.get(prefix);
    }

    @Override
    public void deleteTopic(String prefix) {
        LOG.info("delete topic for {}", prefix);
    }
}
