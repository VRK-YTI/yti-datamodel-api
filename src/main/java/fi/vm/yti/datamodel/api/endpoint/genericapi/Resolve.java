package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.http.client.utils.DateUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
@Path("v1/resolve")
@Tag(name = "Admin")
public class Resolve {

    private static final Logger logger = LoggerFactory.getLogger(Resolve.class);
    private static final String SUOMI_URI_HOST = "uri.suomi.fi";
    private static final String API_PATH_DATAMODEL = "/datamodel/ns/";
    private final GraphManager graphManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final ApplicationProperties applicationProperties;

    @Autowired
    Resolve(GraphManager graphManager,
            JerseyResponseManager jerseyResponseManager,
            ApplicationProperties applicationProperties) {
        this.graphManager = graphManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.applicationProperties = applicationProperties;

    }

    @Context
    UriInfo uriInfo;

    @GET
    @Operation(description = "Redirect URI resource.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "303", description = "Does a redirect from datamodel resource URI to datamodel API or frontend."),
        @ApiResponse(responseCode = "406", description = "Resource not found."),
        @ApiResponse(responseCode = "406", description = "Cannot redirect to given URI.")
    })
    public Response resolveUri(@HeaderParam("Accept") String accept,
                               @HeaderParam("Accept-Language") String acceptLang,
                               @HeaderParam("If-Modified-Since") String ifModifiedSince,
                               @Parameter(description = "Raw / PlainText boolean", schema = @Schema(defaultValue = "false")) @QueryParam("raw") boolean raw,
                               @Parameter(description = "Content-type as format") @QueryParam("format") String format,
                               @Parameter(description = "Resource URI.", required = true) @QueryParam("uri") final String uri) {
        final URI resolveUri = parseUriFromString(uri);
        ensureSuomiFiUriHost(resolveUri.getHost());

        final String uriPath = resolveUri.getPath();
        checkResourceValidity(uriPath);

        final String uriFragment = resolveUri.getFragment();
        final String graphPrefix = uriPath.substring(API_PATH_DATAMODEL.length());

        logger.debug("Resolving: " + uri);

        final String graphName = graphManager.getServiceGraphNameWithPrefix(graphPrefix);

        if (graphName == null) {
            logger.info("Graph not found: " + graphName);
            return Response.status(404).build();
        }

        if (ifModifiedSince != null) {
            logger.debug("If-Modified-Since: " + ifModifiedSince);
            Date modifiedSince = DateUtils.parseDate(ifModifiedSince);
            if (modifiedSince == null) {
                logger.warn("Could not parse If-Modified-Since");
                return jerseyResponseManager.invalidParameter();
            }
            Date modified = graphManager.modelContentModified(graphName);
            if (modified != null) {
                if (modifiedSince.after(modified)) {
                    return Response.notModified().header("Last-Modified", DateUtils.formatDate(modified)).build();
                }
            }
        }

        Locale locale = acceptLang == null ? null : Locale.forLanguageTag(acceptLang);
        String language = locale == null ? null : locale.getDefault().toString().substring(0, 2).toLowerCase();

        final URI htmlRedirectUrl = URI.create(uriInfo.getBaseUri().toString().replace("/datamodel-api/api/", "/model/") + graphPrefix + (uriFragment != null ? "#"+uriFragment : ""));

        if (format != null && format.length() > 5) {
            accept = format;
        }

        if (accept.contains("text/html")) {
            logger.debug("Redirecting to " + htmlRedirectUrl.toString());
            return Response.seeOther(htmlRedirectUrl).build();
        }

        final List<String> acceptHeaders = Arrays.asList(accept.replaceAll("\\s+", "").split(","));

        for (String acceptHeader : acceptHeaders) {

            if (acceptHeader.contains(";")) {
                acceptHeader = acceptHeader.split(";")[0];
            }

            logger.debug("Resolving format: "+acceptHeader);
            Lang rdfLang = RDFLanguages.contentTypeToLang(acceptHeader);

            if (acceptHeader.contains("application/schema+json") || acceptHeader.contains("application/xml")) {
                final URI schemaWithLangURI = URI.create(uriInfo.getBaseUri().toString() + "v1/exportModel?graph=" + graphName + "&content-type=" + acceptHeader + (language == null ? "" : "&lang=" + language) + "&raw=" + raw);
                return Response.seeOther(schemaWithLangURI).build();
            } else if (rdfLang != null) {
                final URI rdfUrl = URI.create(uriInfo.getBaseUri().toString() + "v1/exportModel?graph=" + graphName + "&content-type=" + rdfLang.getHeaderString() + "&raw=" + raw);
                logger.debug("Resolving to RDF: " + rdfUrl);
                return Response.seeOther(rdfUrl).build();
            }
        }

        logger.debug("Unknown accept header, redirecting to ld+json export");
        final URI rdfUrl = URI.create(uriInfo.getBaseUri().toString() + "v1/exportModel?graph=" + graphName + "&content-type=" + Lang.JSONLD.getHeaderString() + "&raw=" + raw);
        return Response.seeOther(rdfUrl).build();

    }

    private void ensureSuomiFiUriHost(final String host) {
        if (!SUOMI_URI_HOST.equalsIgnoreCase(host)) {
            logger.warn("This URI is not resolvable as a datamodel resource, wrong host.");
            throw new BadRequestException("This URI is not resolvable as a datamodel resource.");
        }
    }

    private URI parseUriFromString(final String uriString) {
        if (!uriString.isEmpty()) {
            return URI.create(uriString.replace(" ", "%20"));
        } else {
            logger.warn("URI string was not valid!");
            throw new BadRequestException("URI string was not valid!");
        }
    }

    private void checkResourceValidity(final String uriPath) {
        if (!uriPath.toLowerCase().startsWith(API_PATH_DATAMODEL)) {
            logger.warn("Datamodel resource URI not resolvable, wrong context path!");
            throw new BadRequestException("Datamodel resource URI not resolvable, wrong context path!");
        } else {
            if (!LDHelper.isValidPrefix(uriPath.substring(API_PATH_DATAMODEL.length()))) {
                logger.warn("Could not parse path: " + uriPath);
                throw new BadRequestException("Could not parse graph from uri path");
            }
        }
    }

}
