package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = ResolveController.class)
@ActiveProfiles("junit")
class ResolveControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ResolveController resolveController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.resolveController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    void testRedirectSiteModel() throws Exception {
        var accept = "text/html";
        mvc.perform(get("/v2/resolve")
                        .param("iri", "http://uri.suomi.fi/datamodel/ns/test")
                        .header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/model/test")));
    }

    @Test
    void testRedirectSiteResource() throws Exception {
        var accept = "text/html";
        mvc.perform(get("/v2/resolve")
                        .param("iri", "http://uri.suomi.fi/datamodel/ns/test#someClass")
                        .header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/model/test#someClass")));
    }

    @Test
    void testRedirectSerializedResource() throws Exception {
        var accept = "text/turtle";
        mvc.perform(get("/v2/resolve")
                        .param("iri", "http://uri.suomi.fi/datamodel/ns/test#someClass")
                        .header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/datamodel-api/v2/export/test/someClass")));
    }

    @Test
    void testInvalidIRI() throws Exception {
        var accept = "text/turtle";
        mvc.perform(get("/v2/resolve")
                        .param("iri", "http://invalid.com")
                        .header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().isBadRequest());
    }
}
