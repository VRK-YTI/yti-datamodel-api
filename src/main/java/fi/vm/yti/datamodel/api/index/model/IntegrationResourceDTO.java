package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

public class IntegrationResourceDTO {

    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private String uri;
    private String container;
    private String status;
    private String type;
    private String modified;
    private String created;
    private String statusModified;

    public IntegrationResourceDTO(IndexResourceDTO resource) {
        this.prefLabel = resource.getLabel();
        this.description = resource.getComment();
        this.uri = resource.getId();
        this.container = resource.getIsDefinedBy();
        this.status = resource.getStatus();
        this.statusModified = resource.getStatusModified();
        this.type = resource.getType();
        this.modified = resource.getModified();
        this.created = resource.getCreated();
    }

    public IntegrationResourceDTO(final Map<String, String> prefLabel,
                                  final Map<String, String> description,
                                  final String uri,
                                  final String status,
                                  final String statusModified,
                                  final String type,
                                  final String modified,
                                  final String created) {
        this.prefLabel = prefLabel;
        this.description = description;
        this.uri = uri;
        this.status = status;
        this.statusModified = statusModified;
        this.type = type;
        this.modified = modified;
        this.created = created;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(final Map<String, String> description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(final String container) {
        this.container = container;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getStatusModified() {
        return statusModified;
    }

    public void setStatusModified(final String statusModified) {
        this.statusModified = statusModified;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "IntegrationResourceDTO{" +
            "prefLabel=" + prefLabel +
            ", description=" + description +
            ", uri='" + uri + '\'' +
            ", container='" + container + '\'' +
            ", status='" + status + '\'' +
            ", type='" + type + '\'' +
            ", modified='" + modified + '\'' +
            ", created='" + created + '\'' +
            ", statusModified='" + statusModified + '\'' +
            '}';
    }
}
