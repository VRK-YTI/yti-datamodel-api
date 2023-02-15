package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.service.NamespaceResolver;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = NamespaceResolveController.class)
@ActiveProfiles("junit")
class NamespaceResolveControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NamespaceResolver namespaceResolver;

    @MockBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    private NamespaceResolveController namespaceResolveController;

    private final YtiUser mockUser = new YtiUser("test@localhost",
            "test",
            "tester",
            UUID.randomUUID(),
            true,
            false,
            LocalDateTime.of(2001, 1, 1, 0,0),
            LocalDateTime.of(2001, 1, 1, 0,0),
            new HashMap<>(Map.of(UUID.randomUUID(), Set.of(Role.ADMIN))),
            "",
            "");

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.namespaceResolveController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(authenticatedUserProvider.getUser()).thenReturn(mockUser);
    }

    @Test
    void shouldTryToResolve() throws Exception {
        this.mvc
                .perform(put("/v2/namespace")
                        .param("namespace", "notrealaddress"))
                .andExpect(status().isOk());
        verify(namespaceResolver).resolveNamespace(anyString());
    }

    @ParameterizedTest
    @MethodSource("reservedNamespaceProvider")
    void shouldFailToResolveReserved(String namespace) throws Exception {
        this.mvc
                .perform(put("/v2/namespace")
                        .param("namespace", namespace))
                .andExpect(result -> assertTrue(Objects.requireNonNull(result.getResolvedException()).getMessage().contains("Reserved namespace")))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> reservedNamespaceProvider(){
        return ValidationConstants.RESERVED_NAMESPACES.values().stream().map(Arguments::of);
    }

    @Test
    void testForceResolution() throws Exception {
        when(namespaceResolver.namespaceAlreadyResolved(anyString())).thenReturn(true);

        this.mvc
                .perform(put("/v2/namespace")
                        .param("namespace", "notrealaddress"))
                .andExpect(status().isBadRequest());

        this.mvc
                .perform(put("/v2/namespace")
                        .param("namespace", "notrealaddress")
                        .param("force", "true"))
                .andExpect(status().isOk());
    }
}
