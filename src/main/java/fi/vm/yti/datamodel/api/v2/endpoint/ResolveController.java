package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("v2/resolve")
@Tag(name = "Resolve" )
public class ResolveController {
    private static final Logger LOG = LoggerFactory.getLogger(ResolveController.class);
    static final IRIFactory iriFactory = IRIFactory.iriImplementation();

    @Operation(summary = "Resolve content by its IRI")
    @ApiResponse(responseCode = "303", description = "Resolves given uri and redirects the request per accept header")
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @Parameter(name = "iri", description = "Resource IRI")
    @GetMapping
    public ResponseEntity<String> resolve(@RequestParam String iri, @RequestHeader(value = HttpHeaders.ACCEPT) String accept) {
        LOG.info("Resolve resource {}, accept: {}", iri, accept);

        if (!checkIRI(iriFactory.create(iri))) {
            return ResponseEntity.badRequest().build();
        }

        String resourcePath = iri.substring(ModelConstants.SUOMI_FI_NAMESPACE.length());
        var parts = resourcePath.split("\\W");
        String resource = null;

        if (parts.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        var modelPrefix = parts[0];
        if (parts.length == 2) {
            // single resource
            resource = parts[1];
        }

        var currentUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        var redirectURL = new StringBuilder();
        redirectURL.append(currentUrl.getScheme())
                .append("://")
                .append(currentUrl.getHost())
                .append(currentUrl.getHost().equals("localhost") ? ":3000" : "");

        if (accept != null && accept.contains(MimeTypeUtils.TEXT_HTML_VALUE)) {
            // redirect to the site
            redirectURL
                .append("/model/")
                .append(modelPrefix)
                .append(resource != null ? "#" + resource : "");

        } else {
            // redirect to serialized resource
            redirectURL
                .append("/datamodel-api/v2/export/")
                .append(modelPrefix)
                .append(resource != null ? "/" + resource : "");
        }

        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, redirectURL.toString())
                .build();
    }

    private static boolean checkIRI(IRI iri) {
        return !iri.hasViolation(false)
                && iri.toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
    }
}
