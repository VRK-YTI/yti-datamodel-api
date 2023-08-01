package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResourceInfo;
import fi.vm.yti.datamodel.api.v2.service.ClassService;
import fi.vm.yti.datamodel.api.v2.validator.ValidClass;
import fi.vm.yti.datamodel.api.v2.validator.ValidNodeShape;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/class")
@Tag(name = "Class" )
@Validated
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService){
        this.classService = classService;
    }

    @Operation(summary = "Add a class to a model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Class added to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/library/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createClass(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                              @RequestBody @Parameter(description = "Class data") @ValidClass ClassDTO classDTO) throws URISyntaxException {
        var classUri = classService.create(prefix, classDTO, false);
        return ResponseEntity.created(classUri).build();
    }

    @Operation(summary = "Add a node shape to a model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Node shape added to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/profile/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createNodeShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                  @RequestBody @Parameter(description = "Node shape data") @ValidNodeShape NodeShapeDTO nodeShapeDTO) throws URISyntaxException {
        var classUri = classService.create(prefix, nodeShapeDTO, true);
        return ResponseEntity.created(classUri).build();
    }

    @Operation(summary = "Update a class in a library")
    @ApiResponse(responseCode =  "204", description = "Class updated in model successfully")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Class updated to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Class not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/library/{prefix}/{classIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateClass(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                            @PathVariable @Parameter(description = "Class identifier") String classIdentifier,
                                            @RequestBody @Parameter(description = "Class data") @ValidClass(updateClass = true) ClassDTO classDTO){
        classService.update(prefix, classIdentifier, classDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a node shape in a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Node shape updated to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Property shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/profile/{prefix}/{nodeShapeIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateNodeShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                @PathVariable @Parameter(description = "Node shape identifier") String nodeShapeIdentifier,
                                                @RequestBody @Parameter(description = "Node shape data") @ValidNodeShape(updateNodeShape = true) NodeShapeDTO nodeShapeDTO){
        classService.update(prefix, nodeShapeIdentifier, nodeShapeDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a class from a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class found successfully"),
            @ApiResponse(responseCode = "404", description = "Class not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = "/library/{prefix}/{classIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ClassInfoDTO getClass(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                 @PathVariable @Parameter(description = "Class identifier") String classIdentifier){
        return (ClassInfoDTO) classService.get(prefix, classIdentifier);
    }

    @Operation(summary = "Get a node shape from a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node shape found successfully"),
            @ApiResponse(responseCode = "404", description = "Node shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })    @GetMapping(value = "/profile/{prefix}/{nodeShapeIdentifier}", produces = APPLICATION_JSON_VALUE)
    public NodeShapeInfoDTO getNodeShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                         @PathVariable @Parameter(description = "Node shape identifier") String nodeShapeIdentifier){
        return (NodeShapeInfoDTO) classService.get(prefix, nodeShapeIdentifier);
    }


    @Operation(summary = "Check if identifier for resource already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether a resource with given identifier exists")
    @GetMapping(value = "/{prefix}/{identifier}/exists", produces = APPLICATION_JSON_VALUE)
    public Boolean freeIdentifier(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                  @PathVariable @Parameter(description = "Resource identifier") String identifier) {
        return classService.exists(prefix, identifier);
    }

    @Operation(summary = "Delete a class from a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Class cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @DeleteMapping(value = "/library/{prefix}/{classIdentifier}")
    public void deleteClass(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                            @PathVariable @Parameter(description = "Class identifier") String classIdentifier){
        classService.delete(prefix, classIdentifier);
    }

    @Operation(summary = "Delete a node shape from a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node shape deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Node shape cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @DeleteMapping(value = "/profile/{prefix}/{classIdentifier}")
    public void deleteNodeShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                @PathVariable @Parameter(description = "Class identifier") String classIdentifier){
        classService.delete(prefix, classIdentifier);
    }

    @Operation(summary = "Add property reference to a node shape")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property added to node shape successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Node shape or property shape cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @ApiResponse(responseCode = "200", description = "Property reference deleted successfully")
    @PutMapping(value = "/profile/{prefix}/{nodeShapeIdentifier}/properties")
    public void addNodeShapePropertyReference(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                              @PathVariable @Parameter(description = "Node shape identifier") String nodeShapeIdentifier,
                                              @RequestParam @Parameter(description = "Property shape ") String uri) {
        classService.handlePropertyShapeReference(prefix, nodeShapeIdentifier, uri, false);
    }

    @Operation(summary = "Delete property reference from node shape")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property reference deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Node shape or property shape cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @DeleteMapping(value = "/profile/{prefix}/{nodeShapeIdentifier}/properties")
    public void deleteNodeShapePropertyReference(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                 @PathVariable @Parameter(description = "Node shape identifier") String nodeShapeIdentifier,
                                                 @RequestParam @Parameter(description = "Property shape URI") String uri) {
        classService.handlePropertyShapeReference(prefix, nodeShapeIdentifier, uri, true);
    }

    @Operation(summary = "Get an external class from imports")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "External class found successfully"),
            @ApiResponse(responseCode = "404", description = "External class not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = "/external", produces = APPLICATION_JSON_VALUE)
    public ExternalClassDTO getExternalClass(@RequestParam @Parameter(description = "External class URI") String uri) {
        return classService.getExternal(uri);
    }

    @Operation(summary = "Get all node shapes based on given targetClass")
    @ApiResponse(responseCode = "200", description = "List of node shapes fetched successfully")
    @GetMapping(value = "/nodeshapes", produces = APPLICATION_JSON_VALUE)
    public Collection<IndexResourceInfo> getNodeShapes(@RequestParam @Parameter(description = "Target class") String targetClass) throws IOException {
        return classService.getNodeShapes(targetClass);
    }

    @Operation(summary = "Get all node shapes properties based on sh:node reference")
    @ApiResponse(responseCode = "200", description = "List of node shape's properties fetched successfully")
    @GetMapping(value = "/nodeshape/properties", produces = APPLICATION_JSON_VALUE)
    public Collection<IndexResource> getNodeShapeProperties(@RequestParam @Parameter(description = "Node shape URI") String nodeUri) throws IOException {
        return classService.getNodeShapeProperties(nodeUri);
    }

    @Operation(summary = "Toggles deactivation of a single property shape")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property shape activation toggled successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Property shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/toggle-deactivate/{prefix}", produces = APPLICATION_JSON_VALUE)
    public void deactivatePropertyShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                        @RequestParam @Parameter(description = "Property shape uri") String propertyUri) {
        classService.togglePropertyShape(prefix, propertyUri);
    }

}
