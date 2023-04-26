package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = ExportController.class)
@ActiveProfiles("junit")
class ExportControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JenaService jenaService;

    @MockBean
    private AuthorizationManager authorizationManager;

    @Autowired
    private ExportController exportController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.exportController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    void shouldGetModelAsFileNotSupportedType() throws Exception {
        mvc.perform(get("/v2/export/test")
                        .header("Accept", "application/pdf"))
                .andExpect(status().isNotAcceptable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/ld+json", "text/turtle", "application/rdf+xml"})
    void shouldGetModelWithAcceptHeader(String accept) throws Exception {
        when(jenaService.getDataModel(anyString())).thenReturn(ModelFactory.createDefaultModel());

        mvc.perform(get("/v2/export/test")
                        .header("Accept", accept))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(accept));
        verify(jenaService).getDataModel(anyString());
        verifyNoMoreInteractions(jenaService);
    }

    @Test
    void shouldRemoveTriplesHiddenFromUnauthenticatedUser() throws Exception {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(false);

        mvc.perform(get("/v2/export/test")
                        .header("Accept", "text/turtle"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("skos:editorialNote"))));
    }
}
