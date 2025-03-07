package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.validator.ValidSemanticVersion;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
            produces = {"application/ld+json;charset=utf-8", "text/turtle;charset=utf-8", "application/rdf+xml;charset=utf-8", "application/vnd+oai+openapi+json;charset=utf-8", "application/schema+json"})
    public ResponseEntity<String> export(@PathVariable @Parameter(description = "Data model prefix") String prefix,
                                         @RequestParam(required = false) @Parameter(description = "Version") @ValidSemanticVersion String version,
                                         @RequestParam(required = false) @Parameter(description = "Content type") String contentType,
                                         @RequestParam(required = false) @Parameter(description = "Content language") String language,
                                         @RequestHeader(value = HttpHeaders.ACCEPT) String accept){
        var showAsFile = false;
        var type = accept;

        if (contentType != null) {
            type = URLDecoder.decode(contentType, StandardCharsets.UTF_8);
            showAsFile = true;
        }
        return dataModelService.export(prefix, version, type, showAsFile, language);
    }
}
