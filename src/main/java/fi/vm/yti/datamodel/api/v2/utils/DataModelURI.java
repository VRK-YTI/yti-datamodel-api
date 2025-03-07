package fi.vm.yti.datamodel.api.v2.utils;

import fi.vm.yti.common.Constants;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;

import java.util.Objects;
import java.util.regex.Pattern;

public class DataModelURI {

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

    private final String modelId;
    private final String resourceId;
    private final String version;
    private final String namespace;
    private final String fileName;

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

    private DataModelURI(String modelId, String version, String resourceId) {
        this.modelId = modelId;
        this.resourceId = resourceId;
        this.version = version;
        this.namespace = getModelURI();
        this.fileName = null;
    }

    private DataModelURI(String uri) {
        var matcher = iriPattern.matcher(uri);
        if (matcher.matches()) {
            this.modelId = matcher.group("modelId");
            this.version = matcher.group("version");
            var res = matcher.group("resource");
            if (res != null && res.contains(".")) {
                this.fileName = res;
                this.resourceId = null;
            } else {
                this.fileName = null;
                this.resourceId = res;
            }
            this.namespace = getModelURI();
        } else {
            var u = NodeFactory.createURI(uri);
            this.namespace = u.getNameSpace();
            this.resourceId = u.getLocalName();
            this.modelId = null;
            this.version = null;
            this.fileName = null;
        }
    }

    /**
       Resource's URI as stored in Fuseki
     */
    public String getResourceURI() {
        if (this.resourceId == null) {
            return null;
        }

        if (this.modelId == null) {
            return this.getNamespace() + this.resourceId;
        }
        return getModelURI() + this.resourceId;
    }

    /**
     * Resource's URI with version
     */
    public String getResourceVersionURI() {
        if (this.resourceId == null) {
            return null;
        }
        return getGraphURI() + this.resourceId;
    }

    /**
     * Model resource. Used when fetching model's metadata, e.g. status, languages etc.
     */
    public String getModelURI() {
        return Constants.DATA_MODEL_NAMESPACE + this.modelId + Constants.RESOURCE_SEPARATOR;
    }

    public String getDraftGraphURI() {
        return Constants.DATA_MODEL_NAMESPACE + this.modelId + Constants.RESOURCE_SEPARATOR;
    }

    /**
     * Graph's URI, used when fetching and putting data to Fuseki
     */
    public String getGraphURI() {
        var uri = this.modelId != null
                ? getModelURI()
                : this.namespace;

        if (this.version != null) {
            return uri + this.version + Constants.RESOURCE_SEPARATOR;
        }
        return uri;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public String getModelId() {
        return this.modelId;
    }

    public String getVersion() {
        return this.version;
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
            return String.format(template, prefix, this.resourceId);
        } else if (isDataModelURI()) {
            return String.format(template, this.modelId, this.resourceId);
        } else {
            // use last element of the path as a prefix, if it could not determine
            var parts = this.namespace.split("\\W");
            return String.format(template, parts[parts.length - 1], this.resourceId);
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
        return Objects.equals(modelId, that.modelId) && Objects.equals(resourceId, that.resourceId) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, resourceId, version);
    }
}
