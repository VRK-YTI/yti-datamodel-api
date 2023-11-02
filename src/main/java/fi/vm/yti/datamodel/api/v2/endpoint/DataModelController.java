package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.validator.ValidDatamodel;
import fi.vm.yti.datamodel.api.v2.validator.ValidSemanticVersion;
import fi.vm.yti.datamodel.api.v2.validator.ValidVersionedDatamodel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/model")
@Tag(name = "Model" )
@Validated
public class DataModelController {

    private final DataModelService dataModelService;

    public DataModelController(DataModelService dataModelService) {
        this.dataModelService = dataModelService;
    }

    @Operation(summary = "Create a new library")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The URI for the newly created model"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(path = "/library", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createLibrary(@ValidDatamodel(modelType = ModelType.LIBRARY) @RequestBody DataModelDTO modelDTO) throws URISyntaxException {
        var uri = dataModelService.create(modelDTO, ModelType.LIBRARY);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Create a new application profile")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new application profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The URI for the newly created model"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(path = "/profile", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProfile(@ValidDatamodel(modelType = ModelType.PROFILE) @RequestBody DataModelDTO modelDTO) throws URISyntaxException {
        var uri = dataModelService.create(modelDTO, ModelType.PROFILE);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Modify library")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Library updated successfully"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Library was not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
    })
    @PutMapping(path = "/library/{prefix}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateLibrary(@ValidDatamodel(modelType = ModelType.LIBRARY, updateModel = true) @RequestBody DataModelDTO modelDTO,
                                           @PathVariable @Parameter(description = "Library prefix") String prefix) {
        dataModelService.update(prefix, modelDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Modify application profile")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Application profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Application profile was not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
    })
    @PutMapping(path = "/profile/{prefix}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateProfile(@ValidDatamodel(modelType = ModelType.PROFILE, updateModel = true) @RequestBody DataModelDTO modelDTO,
                              @PathVariable @Parameter(description = "Application profile prefix") String prefix) {
        dataModelService.update(prefix, modelDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a model from fuseki")
    @ApiResponse(responseCode = "200", description = "Datamodel object for the found model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data model found"),
            @ApiResponse(responseCode = "404", description = "Data model was not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
    })
    @GetMapping(value = "/{prefix}", produces = APPLICATION_JSON_VALUE)
    public DataModelInfoDTO getModel(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                     @RequestParam(required = false) @Parameter(description = "Model version, if empty gets the draft version") @ValidSemanticVersion String version) {
        return dataModelService.get(prefix, version);
    }

    @Operation(summary = "Check if prefix already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether prefix")
    @GetMapping(value = "/{prefix}/exists", produces = APPLICATION_JSON_VALUE)
    public Boolean exists(@PathVariable @Parameter(description = "Data model prefix") String prefix) {
        return dataModelService.exists(prefix);
    }

    @Operation(summary = "Delete a model from fuseki")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data model deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Data model was not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
    })
    @DeleteMapping(value = "/{prefix}")
    public void deleteModel(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                            @RequestParam(required = false) @Parameter(description = "Model version, if empty deletes the draft version") @ValidSemanticVersion String version) {
        dataModelService.delete(prefix, version);
    }

    @Operation(summary = "Create a release of a model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Release created successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Data model was not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
    })
    @PostMapping(value = "/{prefix}/release")
    public ResponseEntity<String> createRelease(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                @RequestParam @Parameter(description = "Semantic version") @ValidSemanticVersion String version,
                                                @RequestParam @Parameter(description = "Status") Status status) throws URISyntaxException {
        var uri = dataModelService.createRelease(prefix, version, status);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Get version information of a model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prior versions found"),
    })
    @GetMapping(value = "/{prefix}/versions")
    public ResponseEntity<Collection<ModelVersionInfo>> getPriorVersions(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                                         @RequestParam(required = false) @Parameter(description = "Semantic version") @ValidSemanticVersion String version) {
        return ResponseEntity.ok(dataModelService.getPriorVersions(prefix, version));
    }

    @Operation(summary = "Update versioned model")
    @PutMapping(value = "/{prefix}/version")
    public ResponseEntity<Void> updateVersionedModel(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                     @RequestParam @Parameter(description = "Semantic version") @ValidSemanticVersion String version,
                                                     @ValidVersionedDatamodel @RequestBody VersionedModelDTO dto) {
        dataModelService.updateVersionedModel(prefix, version, dto);
        return ResponseEntity.noContent().build();
    }


}
