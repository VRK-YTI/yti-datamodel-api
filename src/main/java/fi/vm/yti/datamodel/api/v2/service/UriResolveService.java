package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.SemVer;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.topbraid.shacl.vocabulary.SH;

@Service
public class UriResolveService {

    private final Logger logger = LoggerFactory.getLogger(UriResolveService.class);
    private static final IRIFactory iriFactory = IRIFactory.iriImplementation();
    private final CoreRepository coreRepository;

    public UriResolveService(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    public ResponseEntity<String> resolve(String iri, String accept) {
        logger.info("Resolve resource {}, accept: {}", iri, accept);

        if (!checkIRI(iriFactory.create(iri))) {
            return ResponseEntity.badRequest().build();
        }

        String resourcePath = iri.substring(ModelConstants.SUOMI_FI_NAMESPACE.length())
                .split("\\?")[0];
        var parts = resourcePath.split("/");

        if (parts.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        var redirectURL = buildURL(parts, accept);
        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, redirectURL)
                .build();
    }

    @NotNull
    private String buildURL(String[] parts, String accept) {
        String modelPrefix = parts[0];
        String resource = null;
        String version = null;

        for (var i = 1; i < parts.length; i++) {
            var p = parts[i];
            if (i == 1) {
                if (p.matches(SemVer.VALID_REGEX)) {
                    version = p;
                } else {
                    resource = p;
                }
            } else if (i == 2) {
                resource = p;
            }
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
                    .append(modelPrefix);
            appendResource(redirectURL, modelPrefix, resource);
            redirectURL.append(version != null ? "?ver=" + version : "");
        } else {
            // redirect to serialized resource
            redirectURL
                    .append("/datamodel-api/v2/export/")
                    .append(modelPrefix)
                    .append(resource != null ? "/" + resource : "")
                    .append(version != null ? "?version=" + version : "");
        }
        return redirectURL.toString();
    }

    private void appendResource(StringBuilder redirectURL, String modelPrefix, String resource) {
        if (resource != null) {
            var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + modelPrefix;
            Model dataModel;
            try {
                dataModel = coreRepository.fetch(modelURI);
            } catch (Exception e) {
                return;
            }

            var dataModelResource = dataModel.getResource(modelURI + ModelConstants.RESOURCE_SEPARATOR + resource);

            if (MapperUtils.hasType(dataModelResource, OWL.Class, SH.NodeShape)) {
                redirectURL.append("/class/");
            } else if (MapperUtils.hasType(dataModelResource, OWL.DatatypeProperty)) {
                redirectURL.append("/attribute/");
            } else if (MapperUtils.hasType(dataModelResource, OWL.ObjectProperty)) {
                redirectURL.append("/association/");
            } else {
                logger.warn("No valid type found from resource {}, {}", dataModelResource.getURI(),
                        dataModelResource.getProperty(RDF.type));
                return;
            }
            redirectURL.append(resource);
        }
    }

    private static boolean checkIRI(IRI iri) {
        return !iri.hasViolation(false)
                && iri.toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
    }
}
