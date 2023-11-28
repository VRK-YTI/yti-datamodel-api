package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
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
import org.springframework.web.util.UriComponentsBuilder;
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

        var datamodelURI = DataModelURI.fromURI(iri);
        if (datamodelURI.getModelId() == null) {
            return ResponseEntity.badRequest().build();
        } else if (datamodelURI.getContentType() == null && datamodelURI.getResourceId() != null && !isHtml(accept)) {
            // do not support serializing single resource
            return ResponseEntity.notFound().build();
        } else if (datamodelURI.getContentType() == null && datamodelURI.getResourceId() == null && !iri.endsWith(ModelConstants.RESOURCE_SEPARATOR)) {
            // iri must end with slash except when requesting with file extension, e.g. /model/ns/1.2.3/ns.ttl
            return ResponseEntity.notFound().build();
        }
        var contentType = datamodelURI.getContentType() != null
                ? datamodelURI.getContentType()
                : accept;
        var redirectURL = buildURL(datamodelURI, contentType);
        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, redirectURL)
                .build();
    }

    @NotNull
    private String buildURL(DataModelURI uri, String accept) {
        String modelPrefix = uri.getModelId();
        String version = uri.getVersion();

        var currentUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        var redirectURL = UriComponentsBuilder.newInstance()
                .scheme(currentUrl.getScheme())
                .host(currentUrl.getHost());

        if (currentUrl.getHost().equals("localhost")) {
            redirectURL.port(3000);
        }

        if (isHtml(accept)) {
            // redirect to the site
            redirectURL.pathSegment("model", modelPrefix);
            appendResource(redirectURL, modelPrefix, uri.getResourceId(), version);
            if (version != null) {
                redirectURL.queryParam("ver", version);
            }
        } else {
            // redirect to serialized resource
            redirectURL.pathSegment("datamodel-api", "v2", "export", modelPrefix);

            if (uri.getContentType() != null) {
                redirectURL.queryParam("contentType", uri.getContentType().replace("+", "%2B"));
            }
            if (version != null) {
                redirectURL.queryParam("version", version);
            }
        }
        return redirectURL.toUriString();
    }

    private static boolean isHtml(String accept) {
        return accept != null && accept.contains(MimeTypeUtils.TEXT_HTML_VALUE);
    }

    private void appendResource(UriComponentsBuilder redirectURL, String modelPrefix, String resource, String version) {
        if (resource != null) {
            var uri = DataModelURI.createResourceURI(modelPrefix, resource, version);
            Model dataModel;
            try {
                dataModel = coreRepository.fetch(uri.getGraphURI());
            } catch (Exception e) {
                return;
            }

            var dataModelResource = dataModel.getResource(uri.getResourceURI());

            if (MapperUtils.hasType(dataModelResource, OWL.Class, SH.NodeShape)) {
                redirectURL.pathSegment("class");
            } else if (MapperUtils.hasType(dataModelResource, OWL.DatatypeProperty)) {
                redirectURL.pathSegment("attribute");
            } else if (MapperUtils.hasType(dataModelResource, OWL.ObjectProperty)) {
                redirectURL.pathSegment("association");
            } else {
                logger.warn("No valid type found from resource {}, {}", dataModelResource.getURI(),
                        dataModelResource.getProperty(RDF.type));
                return;
            }
            redirectURL.pathSegment(resource);
        }
    }

    private static boolean checkIRI(IRI iri) {
        return !iri.hasViolation(false)
                && iri.toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
    }
}
