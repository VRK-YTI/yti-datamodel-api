package fi.vm.yti.datamodel.api.v2.service.sns;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Profile({"dev", "test", "prod", "localSNS"})
public class SubscriptionService implements SnsService {

    private final SnsClient snsClient;

    @Value("${notifications.topic.prefix:arn:aws:sns:eu-west-1:548279152305:}")
    private String snsTopicArnPrefix;

    @Value("${notifications.topic.type:datamodel}")
    private String subscriptionType;

    private final Cache<String, List<Subscription>> subscriptionCache;

    private static final String PROTOCOL = "email";

    public SubscriptionService(@Value("${spring.profiles.active}") String profiles) {

        var snsClientBuilder = SnsClient.builder()
                .region(Region.EU_WEST_1);

        if (profiles.contains("localSNS")) {
            // If you need to test sending notifications from local environment,
            // switch to profile localSNS and run following commands before starting the application
            // aws sso login --profile dvv-yti-dev
            // export AWS_PROFILE=dvv-yti-dev
            snsClientBuilder.credentialsProvider(
                    ProfileCredentialsProvider.builder().profileName("dvv-yti-dev").build());
        }

        this.snsClient = snsClientBuilder.build();

        this.subscriptionCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    @Override
    public String subscribe(String prefix, String email) {
        var createTopic = CreateTopicRequest.builder().name(subscriptionType + "-" + prefix).build();
        snsClient.createTopic(createTopic);

        var request = SubscribeRequest.builder()
                .protocol(PROTOCOL)
                .endpoint(email)
                .topicArn(getTopicArn(prefix))
                .returnSubscriptionArn(true)
                .build();

        var response = snsClient.subscribe(request);

        subscriptionCache.invalidate(prefix);

        return response.subscriptionArn();
    }

    @Override
    public void unsubscribe(String subscriptionARN) {
        var req = UnsubscribeRequest.builder().subscriptionArn(subscriptionARN).build();
        snsClient.unsubscribe(req);
    }

    @Override
    public void publish(String prefix, String title, String message) {
        var req = PublishRequest.builder()
                .message(message)
                .subject(title)
                .topicArn(getTopicArn(prefix))
                .build();

        snsClient.publish(req);
    }

    @Override
    public List<Subscription> listSubscriptions(String email) {
        var req = ListSubscriptionsRequest.builder().build();

        return snsClient.listSubscriptionsPaginator(req).subscriptions().stream()
                .filter(s -> s.endpoint().equals(email))
                .toList();
    }

    @Override
    public String getSubscription(String prefix, String email) {
        try {
            List<Subscription> subscriptions = subscriptionCache.getIfPresent(prefix);
            if (subscriptions == null) {
                var req = ListSubscriptionsByTopicRequest.builder()
                        .topicArn(getTopicArn(prefix))
                        .build();
                subscriptions = snsClient.listSubscriptionsByTopicPaginator(req).subscriptions().stream().toList();
                subscriptionCache.put(prefix, subscriptions);
            }
            return subscriptions.stream()
                    .filter(s -> s.endpoint().equals(email))
                    .findFirst()
                    .map(Subscription::subscriptionArn)
                    .orElse(null);
        } catch (SnsException se) {
            subscriptionCache.put(prefix, List.of());
            return null;
        }
    }

    @Override
    public void deleteTopic(String prefix) {
        var arn = getTopicArn(prefix);
        var deleteRequest = DeleteTopicRequest.builder().topicArn(arn).build();
        snsClient.deleteTopic(deleteRequest);
    }

    private String getTopicArn(String prefix) {
        return snsTopicArnPrefix + subscriptionType + "-" + prefix;
    }
}
