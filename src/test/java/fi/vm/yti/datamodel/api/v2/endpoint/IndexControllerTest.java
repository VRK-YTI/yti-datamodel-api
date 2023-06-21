package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private AuthorizationManager authorizationManager;

    @MockBean
    private OpenSearchIndexer openSearchIndexer;

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
    void shouldReIndex() throws Exception {
        this.mvc
            .perform(get("/v2/index/reindex"))
            .andExpect(status().isOk());

        verify(this.openSearchIndexer).reindex();
    }

    @Test
    void shouldReIndexParameter() throws Exception {
        this.mvc
                .perform(get("/v2/index/reindex")
                        .param("index", "models_v2"))
                .andExpect(status().isOk());
        verify(openSearchIndexer).initModelIndex();

        this.mvc
                .perform(get("/v2/index/reindex")
                        .param("index", "resources_v2"))
                .andExpect(status().isOk());
        verify(openSearchIndexer).initResourceIndex();

        this.mvc
                .perform(get("/v2/index/reindex")
                        .param("index", "external_v2"))
                .andExpect(status().isOk());
        verify(openSearchIndexer).initExternalResourceIndex();
        
        this.mvc
		        .perform(get("/v2/index/reindex")
		                .param("index", "crosswalks_v2"))
		        .andExpect(status().isOk());
		verify(openSearchIndexer).initCrosswalkIndex();        
    }

    @Test
    void reindexThrowOnInvalidParamater() throws Exception{
        this.mvc
                .perform(get("/v2/index/reindex")
                        .param("index", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(openSearchIndexer);
    }
}
