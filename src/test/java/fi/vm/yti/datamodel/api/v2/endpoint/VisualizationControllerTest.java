package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.visualization.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
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

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(VisualizationController.class)
@ActiveProfiles("junit")
class VisualizationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private VisualizationService visualizationService;

    @Autowired
    private VisualizationController visualizationController;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(this.visualizationController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    void testGetVisualizationData() throws Exception {
        mvc.perform(get("/v2/visualization/{prefix}", "test"))
                .andExpect(status().isOk());

        verify(visualizationService).getVisualizationData("test", null);

        mvc.perform(get("/v2/visualization/{prefix}", "test")
                        .param("version", "1.0.1"))
                .andExpect(status().isOk());
        verify(visualizationService).getVisualizationData("test", "1.0.1");
    }

    @Test
    void testSavePositions() throws Exception {
        mvc.perform(put("/v2/visualization/{prefix}/positions", "test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(new ArrayList<PositionDataDTO>()))
                )
                .andExpect(status().isNoContent());

        verify(visualizationService).savePositionData(eq("test"), anyList());
    }
}
