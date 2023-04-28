package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.endsWith;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = ResolveController.class)
@ActiveProfiles("junit")
class ResolveControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JenaService jenaService;

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
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(jenaService.getDataModel(anyString())).thenReturn(model);

        var pathMap = Map.of(
                "http://uri.suomi.fi/datamodel/ns/test#TestClass", "/model/test/class/TestClass",
                "http://uri.suomi.fi/datamodel/ns/test#TestAttribute", "/model/test/attribute/TestAttribute",
                "http://uri.suomi.fi/datamodel/ns/test#TestAssociation", "/model/test/association/TestAssociation"
        );
        var accept = "text/html";
        for (var key : pathMap.keySet()) {
            mvc.perform(get("/v2/resolve")
                            .param("iri", key)
                            .header(HttpHeaders.ACCEPT, accept))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string(HttpHeaders.LOCATION, endsWith(pathMap.get(key))));
        }
    }

    @Test
    void testRedirectSerializedResource() throws Exception {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(jenaService.getDataModel(anyString())).thenReturn(model);

        var accept = "text/turtle";
        mvc.perform(get("/v2/resolve")
                        .param("iri", "http://uri.suomi.fi/datamodel/ns/test#TestClass")
                        .header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/datamodel-api/v2/export/test/TestClass")));
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
