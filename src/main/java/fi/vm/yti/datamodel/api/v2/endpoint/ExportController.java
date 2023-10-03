package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v2/export")
@Tag(name = "Export" )
public class ExportController {

    private final DataModelService dataModelService;

    public ExportController(DataModelService dataModelService) {
        this.dataModelService = dataModelService;
    }

    @Operation(summary = "Get a data model or a single resource serialized")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get and serialize resource successfully"),
            @ApiResponse(responseCode = "404", description = "Model or Resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))})
    })
    @GetMapping(value = {"{prefix}", "/{prefix}/{resourceIdentifier}"},
            produces = {"application/ld+json;charset=utf-8", "text/turtle;charset=utf-8", "application/rdf+xml;charset=utf-8"})
    public ResponseEntity<String> export(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                         @PathVariable(required = false) @Parameter(description = "Resource identifier") String resourceIdentifier,
                                         @RequestParam(required = false) @Parameter(description = "Version") String version,
                                         @RequestHeader(value = HttpHeaders.ACCEPT) String accept){
        return dataModelService.export(prefix, version, resourceIdentifier, accept);
    }
}
