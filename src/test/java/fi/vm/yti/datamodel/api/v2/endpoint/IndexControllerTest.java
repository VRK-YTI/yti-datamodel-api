package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.service.IndexService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers =IndexController.class)
@ActiveProfiles("junit")
class IndexControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DataModelAuthorizationManager authorizationManager;

    @MockBean
    private IndexService indexService;

    @Autowired
    private IndexController indexController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.indexController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(authorizationManager.hasRightToDropDatabase()).thenReturn(true);
    }

    @Test
    void shouldCallOpenSearchIndexer() throws Exception {
        mvc.perform(post("/v2/index/reindex"))
            .andExpect(status().isOk());

        verify(indexService).reindex(null);

        mvc.perform(post("/v2/index/reindex")
                    .param("index", "models_v2"))
            .andExpect(status().isOk());

        verify(indexService).reindex("models_v2");
    }
}
