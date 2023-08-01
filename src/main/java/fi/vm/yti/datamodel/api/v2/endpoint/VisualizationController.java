package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationResultDTO;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/visualization")
@Tag(name = "Visualization" )
public class VisualizationController {

    private final VisualizationService visualizationService;

    public VisualizationController(VisualizationService visualizationService) {
        this.visualizationService = visualizationService;
    }

    @Operation(summary = "Get data for model visualization")
    @ApiResponse(responseCode = "200", description = "Visualization data found for model")
    @GetMapping(value = "/{prefix}", produces = APPLICATION_JSON_VALUE)
    public VisualizationResultDTO getVisualizationData(@PathVariable @Parameter(description = "Data model prefix") String prefix) {
        return visualizationService.getVisualizationData(prefix);
    }

    @Operation(summary = "Saves position data for visualization components")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Visualization data saved or updated for the model"),
        @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model")
    })
    @PutMapping(value = "/{prefix}/positions")
    public ResponseEntity<Void> savePositions(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                              @RequestBody @Parameter(description = "List of positions") List<PositionDataDTO> positions) {
        visualizationService.savePositionData(prefix, positions);
        return ResponseEntity.noContent().build();
    }
}
