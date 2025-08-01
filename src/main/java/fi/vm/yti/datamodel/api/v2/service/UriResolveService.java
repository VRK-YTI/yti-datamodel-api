package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final DataModelAuthorizationManager authorizationManager;
    private final DataModelService dataModelService;

    @Value("${env:}")
    private String awsEnv;

    public UriResolveService(CoreRepository coreRepository,
                             DataModelAuthorizationManager authorizationManager, DataModelService dataModelService) {
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        this.dataModelService = dataModelService;
    }

    public ResponseEntity<String> resolve(String iri, String accept) {
        logger.info("Resolve resource {}, accept: {}", iri, accept);

        if (!checkIRI(iriFactory.create(iri))) {
            return ResponseEntity.badRequest().build();
        }

        var datamodelURI = DataModelURI.Factory.fromURI(iri);
        if (datamodelURI.getModelId() == null) {
            return ResponseEntity.badRequest().build();
        } else if (datamodelURI.getContentType() == null && datamodelURI.getResourceId() != null && !isHtml(accept)) {
            // do not support serializing single resource
            return ResponseEntity.notFound().build();
        } else if (datamodelURI.getContentType() == null && datamodelURI.getResourceId() == null && !iri.endsWith(Constants.RESOURCE_SEPARATOR)) {
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

    public String resolveLegacyURL(String modelId, String resourceId) {
        logger.info("Resolving legacy url for {}, {}", modelId, resourceId);

        // handle fragment, e.g. http://uri.suomi.fi/datamodel/ns/model-id#resource-id
        if (modelId.contains("#")) {
            var parts = modelId.split("#");
            modelId = parts[0];
            resourceId = parts[1];
        }

        var uri = DataModelURI.Factory.createModelURI(modelId);

        var model = coreRepository.fetch(uri.getGraphURI());
        var hasRights = authorizationManager.hasRightToModel(uri.getModelId(), model);

        String version = null;
        var versionIRI = MapperUtils.propertyToString(model.getResource(uri.getModelURI()), OWL2.priorVersion);
        if (versionIRI != null && !hasRights) {
            version = DataModelURI.Factory.fromURI(versionIRI).getVersion();
        }

        var resolvedURI = resourceId != null
                ? DataModelURI.Factory.createResourceURI(modelId, resourceId, version).getResourceVersionURI()
                : DataModelURI.Factory.createModelURI(modelId, version).getGraphURI();

        if (!awsEnv.isBlank()) {
            return resolvedURI + "?env=" + awsEnv;
        }

        return resolvedURI;
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
            // redirect to the site (latest if no version specified)
            redirectURL.pathSegment("model", modelPrefix);

            if (version == null) {
                try {
                    version = dataModelService.getLatestVersion(uri);
                } catch (ResourceNotFoundException e) {
                    // no published version -> redirect to draft
                }
            }

            var dataModel = coreRepository.fetch(DataModelURI.Factory.createModelURI(modelPrefix, version).getGraphURI());

            if (uri.getResourceURI() != null) {
                var resource = dataModel.getResource(uri.getResourceURI());
                appendResource(redirectURL, resource, uri.getResourceId());
            }
            if (version != null) {
                redirectURL.queryParam("ver", version);
            } else {
                redirectURL.queryParam("draft");
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

    private void appendResource(UriComponentsBuilder redirectURL, Resource dataModelResource, String resource) {
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

    private static boolean checkIRI(IRI iri) {
        return !iri.hasViolation(false)
                && iri.toString().startsWith(Constants.DATA_MODEL_NAMESPACE);
    }
}
