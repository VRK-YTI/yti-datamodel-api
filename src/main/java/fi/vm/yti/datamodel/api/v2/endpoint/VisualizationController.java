package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.visualization.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.dto.visualization.VisualizationResultDTO;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import fi.vm.yti.datamodel.api.v2.validator.ValidSemanticVersion;
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
    public VisualizationResultDTO getVisualizationData(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                       @RequestParam(required = false) @Parameter(description = "Version of the model") @ValidSemanticVersion String version) {
        return visualizationService.getVisualizationData(prefix, version);
    }

    @Operation(summary = "Saves position data for visualization components")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Visualization data saved or updated for the model"),
        @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model")
    })
    @PutMapping(value = "/{prefix}/positions")
    public ResponseEntity<Void> savePositions(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                              @RequestParam(required = false) @Parameter(description = "Model version, if empty uses the draft version") @ValidSemanticVersion String version,
                                              @RequestBody @Parameter(description = "List of positions") List<PositionDataDTO> positions) {
        visualizationService.savePositionData(prefix, positions, version);
        return ResponseEntity.noContent().build();
    }
}
