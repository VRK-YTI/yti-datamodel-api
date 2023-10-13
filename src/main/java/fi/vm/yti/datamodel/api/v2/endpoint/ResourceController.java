package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import fi.vm.yti.datamodel.api.v2.validator.ValidPropertyShape;
import fi.vm.yti.datamodel.api.v2.validator.ValidResource;
import fi.vm.yti.datamodel.api.v2.validator.ValidResourceIdentifier;
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/resource")
@Tag(name = "Resource" )
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "Add an attribute to a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attribute added to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/library/{prefix}/attribute", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAttribute(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                  @RequestBody @ValidResource(resourceType = ResourceType.ATTRIBUTE) ResourceDTO dto) throws URISyntaxException {
        var uri = resourceService.create(prefix, dto, ResourceType.ATTRIBUTE, false);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Add an association to a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Association added to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/library/{prefix}/association", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAssociation(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                    @RequestBody @ValidResource(resourceType = ResourceType.ASSOCIATION) ResourceDTO dto) throws URISyntaxException {
        var uri = resourceService.create(prefix, dto, ResourceType.ASSOCIATION, false);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Add a attribute restriction to a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attribute restriction added to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/profile/{prefix}/attribute", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAttributeRestriction(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                      @ValidPropertyShape(resourceType = ResourceType.ATTRIBUTE) @RequestBody AttributeRestriction dto) throws URISyntaxException {
        var uri = resourceService.create(prefix, dto, ResourceType.ATTRIBUTE, true);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Add a association restriction to a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Association restriction added to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied or resource with given identifier already exists", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
    })
    @PostMapping(value = "/profile/{prefix}/association", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAssociationRestriction(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                      @ValidPropertyShape(resourceType = ResourceType.ASSOCIATION) @RequestBody AssociationRestriction dto) throws URISyntaxException {
        var uri = resourceService.create(prefix, dto, ResourceType.ASSOCIATION, true);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Update an attribute in a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attribute updated to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Attribute not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/library/{prefix}/attribute/{resourceIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateAttribute(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                @PathVariable @Parameter(description = "Attribute identifier") String resourceIdentifier,
                               @RequestBody @ValidResource(resourceType = ResourceType.ATTRIBUTE, updateProperty = true) ResourceDTO dto){
        resourceService.update(prefix, resourceIdentifier, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update an association in a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Association updated to library successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Association not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/library/{prefix}/association/{resourceIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateAssociation(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                  @PathVariable @Parameter(description = "Association identifier") String resourceIdentifier,
                               @RequestBody @ValidResource(resourceType = ResourceType.ASSOCIATION, updateProperty = true) ResourceDTO dto){
        resourceService.update(prefix, resourceIdentifier, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a attribute restriction in a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Property shape updated to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Property shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/profile/{prefix}/attribute/{propertyShapeIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateAttributeRestriction(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                    @PathVariable @Parameter(description = "Property shape identifier") String propertyShapeIdentifier,
                                    @RequestBody @ValidPropertyShape(resourceType = ResourceType.ATTRIBUTE, updateProperty = true) AttributeRestriction dto){
        resourceService.update(prefix, propertyShapeIdentifier, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a association restriction in a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Association restriction updated to profile successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data supplied", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Association restriction not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PutMapping(value = "/profile/{prefix}/association/{propertyShapeIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateAssociationRestriction(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                    @PathVariable @Parameter(description = "Property shape identifier") String propertyShapeIdentifier,
                                                    @RequestBody @ValidPropertyShape(resourceType = ResourceType.ASSOCIATION, updateProperty = true) AssociationRestriction dto){
        resourceService.update(prefix, propertyShapeIdentifier, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create a local copy of a property shape")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Property shape copied successfully"),
            @ApiResponse(responseCode = "400", description = "Property shape with new identifier already exists", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Property shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PostMapping(value ="/profile/{prefix}/{propertyShapeIdentifier}/copy")
    public ResponseEntity<String> copyPropertyShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                    @PathVariable @Parameter(description = "Property shape identifier") String propertyShapeIdentifier,
                                                    @RequestParam @Parameter(description = "Data model property shape will be copied to") String targetPrefix,
                                                    @RequestParam @Parameter(description = "Identifier of new propertyshape") String newIdentifier) throws URISyntaxException {
        var uri = resourceService.copyPropertyShape(prefix, propertyShapeIdentifier, targetPrefix, newIdentifier);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Find an attribute or association from a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute or association found"),
            @ApiResponse(responseCode = "404", description = "Attribute or association not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = "/library/{prefix}/{resourceIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ResourceInfoDTO getResource(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                       @PathVariable @Parameter(description = "Attribute or association identifier") String resourceIdentifier,
                                       @RequestParam(required = false) @Parameter(description = "Version") String version){
        //TODO check if this can be generic instead of casting
        return (ResourceInfoDTO) resourceService.get(prefix, version, resourceIdentifier);
    }

    @Operation(summary = "Find a property shape from a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property shape found"),
            @ApiResponse(responseCode = "404", description = "Property shape not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = "/profile/{prefix}/{propertyShapeIdentifier}", produces = APPLICATION_JSON_VALUE)
    public PropertyShapeInfoDTO getPropertyShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                                 @PathVariable @Parameter(description = "Property shape identifier") String propertyShapeIdentifier,
                                                 @RequestParam(required = false) @Parameter(description = "Version") String version){
        return (PropertyShapeInfoDTO) resourceService.get(prefix, version, propertyShapeIdentifier);
    }

    @Operation(summary = "Get an external resource from imports")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "External resource found successfully"),
            @ApiResponse(responseCode = "404", description = "External resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = "/external", produces = APPLICATION_JSON_VALUE)
    public ExternalResourceDTO getExternal(@RequestParam @Parameter(description = "Uri of external resource") String uri) {
        return resourceService.getExternal(uri);
    }

    @Operation(summary = "Check if identifier for resource already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether a resource with given identifier exists")
    @GetMapping(value = "/{prefix}/{identifier}/exists", produces = APPLICATION_JSON_VALUE)
    public Boolean exists(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                          @PathVariable @Parameter(description = "Identifier to check") String identifier) {
        return resourceService.exists(prefix, identifier);
    }

    @Operation(summary = "Delete an attribute or association from a library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute or association deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Attribute or association cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @DeleteMapping(value = "/library/{prefix}/{resourceIdentifier}")
    public void deleteResource(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                               @PathVariable @Parameter(description = "Attribute or association identifier") String resourceIdentifier){
        resourceService.delete(prefix, resourceIdentifier);
    }

    @GetMapping(value = "/profile/{prefix}/active")
    public Boolean getActiveStatus(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                   @RequestParam @Parameter(description = "Property shape prefix") String uri){
        return resourceService.checkActiveStatus(prefix, uri);
    }

    @Operation(summary = "Delete a property shape from a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property shape deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Property shape cannot be found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @DeleteMapping(value = "/profile/{prefix}/{propertyShapeIdentifier}")
    public void deletePropertyShape(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                    @PathVariable @Parameter(description = "Property shape identifier") String propertyShapeIdentifier) {
        // TODO: need to check node shapes' sh:property if resource is added there
        resourceService.delete(prefix, propertyShapeIdentifier);
    }


    @Operation(summary = "Renames attribute or association with new identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource renamed successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this model"),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @PostMapping(value = "/{prefix}/{identifier}/rename", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> renameClass(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                              @PathVariable @Parameter(description = "Identifier to be renamed") String identifier,
                                              @RequestParam @Parameter(description = "New identifier") @ValidResourceIdentifier String newIdentifier) throws URISyntaxException {
        var newURI = resourceService.renameResource(prefix, identifier, newIdentifier);
        return ResponseEntity.created(newURI).build();
    }

}
