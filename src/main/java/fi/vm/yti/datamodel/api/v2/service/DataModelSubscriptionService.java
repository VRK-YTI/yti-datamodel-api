package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.SubscriptionDTO;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.sns.SnsService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataModelSubscriptionService {

    private final AuthenticatedUserProvider userProvider;
    private final CoreRepository repository;
    private final SnsService snsService;

    public DataModelSubscriptionService(AuthenticatedUserProvider userProvider,
                                        CoreRepository repository,
                                        SnsService snsService) {
        this.userProvider = userProvider;
        this.repository = repository;
        this.snsService = snsService;
    }

    public void publish(String prefix, String title, String message) {
        snsService.publish(prefix, title, message);
    }

    public String subscribe(String prefix) {
        var user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new AuthorizationException("Not allowed to subscribe");
        }

        var dataModelURI = DataModelURI.Factory.createModelURI(prefix);

        if (!repository.graphExists(dataModelURI.getGraphURI())) {
            throw new ResourceNotFoundException(dataModelURI.getGraphURI());
        }

        return snsService.subscribe(prefix, user.getEmail());
    }

    public List<SubscriptionDTO> listSubscriptions() {
        var user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new AuthorizationException("Not allowed to list subscriptions");
        }

        var subscriptions = snsService.listSubscriptions(user.getEmail()).stream()
                .filter(s -> s.endpoint().equals(user.getEmail()))
                .map(s -> {
                    var dto = new SubscriptionDTO();
                    dto.setModelURI(DataModelURI.Factory.createModelURI(getModelIdFromARN(s.topicArn())).getModelURI());
                    dto.setSubscriptionARN(s.subscriptionArn());
                    return dto;
                }).toList();

        var construct = new ConstructBuilder();

        for (var i = 0; i < subscriptions.size(); i++) {
            var g = ResourceFactory.createResource(subscriptions.get(i).getModelURI());
            construct.addConstruct(g, RDFS.label, "?l" + i);
            construct.addConstruct(g, OWL.priorVersion, "?v" + i);

            var where = new WhereBuilder()
                    .addGraph(g, new WhereBuilder()
                            .addWhere(g, RDFS.label, "?l" + i)
                            .addWhere(g, OWL.priorVersion, "?v" + i));

            construct.addUnion(where);
        }

        var result = repository.queryConstruct(construct.build());

        subscriptions.forEach(s -> {
            var resource = result.getResource(s.getModelURI());
            s.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
            s.setModelURI(MapperUtils.propertyToString(resource, OWL.priorVersion));
        });
        return subscriptions;
    }

    public void unsubscribe(String subscriptionARN) {
        var user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new AuthorizationException("Not allowed to unsubscribe");
        }
        snsService.unsubscribe(subscriptionARN);
    }

    public String getSubscription(String prefix) {
        var user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new AuthorizationException("Not allowed to get subscription");
        }

        return snsService.getSubscription(prefix, user.getEmail());
    }

    public void deleteTopic(String prefix) {
        snsService.deleteTopic(prefix);
    }

    private String getModelIdFromARN(String arn) {
        return arn.split(":datamodel-")[1];
    }
}
