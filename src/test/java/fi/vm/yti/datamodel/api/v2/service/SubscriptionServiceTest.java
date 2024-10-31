package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.sns.SnsService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.security.YtiUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        DataModelSubscriptionService.class
})
class SubscriptionServiceTest {

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    CoreRepository repository;

    @MockBean
    SnsService snsService;

    @Autowired
    DataModelSubscriptionService subscriptionService;

    private static final String GRAPH = DataModelURI.createModelURI("test").getGraphURI();

    @BeforeEach
    void setUp() {
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        when(repository.graphExists(GRAPH)).thenReturn(true);
    }

    @Test
    void testAddSubscription() {
        subscriptionService.subscribe("test");
        verify(snsService).subscribe("test", "test@localhost");
    }

    @Test
    void testAddSubscriptionUnauthenticated() {
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        assertThrows(AuthorizationException.class, () -> subscriptionService.subscribe("test"));
        verify(snsService, never()).subscribe(anyString(), anyString());
    }

    @Test
    void testAddSubscriptionNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.subscribe("not-exists"));
        verify(snsService, never()).subscribe(anyString(), anyString());
    }

    @Test
    void testGetSubscription() {
        subscriptionService.getSubscription("test");
        verify(snsService).getSubscription("test", "test@localhost");
    }
}
