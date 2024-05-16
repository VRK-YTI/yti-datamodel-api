package fi.vm.yti.datamodel.api.v2.service.sns;

import software.amazon.awssdk.services.sns.model.Subscription;

import java.util.List;

public interface SnsService {

    String subscribe(String prefix, String email);

    void unsubscribe(String subscriptionARN);

    void publish(String prefix, String title, String message);

    List<Subscription> listSubscriptions(String email);

    String getSubscription(String prefix, String email);

    void deleteTopic(String prefix);
}
