package fi.vm.yti.datamodel.api.v2.utils;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.GraphURI;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;

import java.util.Objects;
import java.util.regex.Pattern;

public class DataModelURI extends GraphURI {

    /**
     * Pattern for model, resource and serialization URIs
     * /model/test-model/
     * /model/test-model/1.0.0/
     * /model/test-model
     * /model/test-model/1.0.0
     * /model/test-model/some-resources
     * /model/test-model/1.0.0/some-resource
     * /model/test-model/1.0.0/test-model.ttl
     */
    private static final Pattern iriPattern = Pattern.compile("https?://iri.suomi.fi/model/" +
            "(?<modelId>[\\w-]+)/?" +
            "(/(?<version>\\d+\\.\\d+\\.\\d+)/?)?" +
            "(/(?<resource>[\\w-.]+))?");

    private final String namespace;
    private final String fileName;

    private DataModelURI(String modelId, String version, String resourceId) {
        createResourceURI(modelId, resourceId, version);
        this.namespace = getModelURI();
        this.fileName = null;
    }

    private DataModelURI(String uri) {
        var matcher = iriPattern.matcher(uri);
        String resourceId, prefix, version;
        if (matcher.matches()) {
            prefix = matcher.group("modelId");
            version = matcher.group("version");
            var res = matcher.group("resource");
            if (res != null && res.contains(".")) {
                this.fileName = res;
                resourceId = null;
            } else {
                this.fileName = null;
                resourceId = res;
            }
            createResourceURI(prefix, resourceId, version);
            this.namespace = getModelResourceURI();
        } else {
            var u = NodeFactory.createURI(uri);
            createResourceURI(null, u.getLocalName(), null);
            this.namespace = u.getNameSpace();
            this.fileName = null;
        }
    }

    /**
       Resource's URI as stored in Fuseki
     */
    public String getResourceURI() {
        if (this.getResourceId() == null) {
            return null;
        }

        if (this.getModelId() == null) {
            return this.getNamespace() + this.getResourceId();
        }
        return getModelURI() + this.getResourceId();
    }

    /**
     * Resource's URI with version
     */
    public String getResourceVersionURI() {
        if (this.getResourceId() == null) {
            return null;
        }
        return getGraphURI() + this.getResourceId();
    }

    /**
     * Model resource. Used when fetching model's metadata, e.g. status, languages etc.
     */
    public String getModelURI() {
        return Constants.DATA_MODEL_NAMESPACE + this.getModelId() + Constants.RESOURCE_SEPARATOR;
    }

    public String getModelResourceURI() {
        return Constants.DATA_MODEL_NAMESPACE + this.getModelId() + Constants.RESOURCE_SEPARATOR;
    }

    public String getDraftGraphURI() {
        return Constants.DATA_MODEL_NAMESPACE + this.getModelId() + Constants.RESOURCE_SEPARATOR;
    }

    /**
     * Graph's URI, used when fetching and putting data to Fuseki
     */
    public String getGraphURI() {
        var uri = this.getModelId() != null
                ? getModelURI()
                : this.namespace;

        if (this.getVersion() != null) {
            return uri + this.getVersion() + Constants.RESOURCE_SEPARATOR;
        }
        return uri;
    }

    // for compatibility with old code
    public String getModelId() {
        return this.getPrefix();
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getContentType() {
        if (this.fileName == null) {
            return null;
        }

        var suffix = this.fileName.split("\\.")[1];

        return switch (suffix) {
            case "ttl" -> "text/turtle";
            case "rdf", "xml" -> "application/rdf+xml";
            case "jsonld", "json" -> "application/ld+json";
            default -> null;
        };
    }

    public String getCurie(PrefixMapping prefixMapping) {
        var prefix = prefixMapping.getNsURIPrefix(this.namespace);
        var template = "%s:%s";

        if (prefix != null) {
            return String.format(template, prefix, this.getResourceId());
        } else if (isDataModelURI()) {
            return String.format(template, this.getModelId(), this.getResourceId());
        } else {
            // use last element of the path as a prefix, if it could not determine
            var parts = this.namespace.split("\\W");
            return String.format(template, parts[parts.length - 1], this.getResourceId());
        }
    }

    public boolean isDataModelURI() {
        return this.namespace.startsWith(Constants.DATA_MODEL_NAMESPACE);
    }

    public boolean isCodeListURI() {
        return this.namespace.startsWith(ModelConstants.CODELIST_NAMESPACE);
    }

    public boolean isTerminologyURI() {
        return this.namespace.startsWith(Constants.TERMINOLOGY_NAMESPACE);
    }

    public boolean isSameModel(DataModelURI other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(other.getModelId(), this.getModelId())
                && !Objects.equals(other.getVersion(), this.getVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataModelURI that = (DataModelURI) o;
        return Objects.equals(this.getModelId(), that.getModelId()) && Objects.equals(this.getResourceId(), that.getResourceId()) && Objects.equals(this.getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getModelId(), this.getResourceId(), this.getVersion());
    }

    public static class Factory {
        public static DataModelURI createModelURI(String modelId, String version) {
            return new DataModelURI(modelId, version, null);
        }

        public static DataModelURI createModelURI(String modelId) {
            return new DataModelURI(modelId, null, null);
        }

        public static DataModelURI createResourceURI(String modelId, String resourceId, String version) {
            return new DataModelURI(modelId, version, resourceId);
        }

        public static DataModelURI createResourceURI(String modelId, String resourceId) {
            return new DataModelURI(modelId, null, resourceId);
        }

        public static DataModelURI fromURI(String resourceURI) {
            return new DataModelURI(resourceURI);
        }
    }
}
