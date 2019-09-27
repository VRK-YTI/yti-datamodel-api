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

    public IntegrationResourceDTO(IndexResourceDTO resource) {
        this.prefLabel = resource.getLabel();
        this.description = resource.getComment();
        this.uri = resource.getId();
        this.container = resource.getIsDefinedBy();
        this.status = resource.getStatus();
        this.type = resource.getType();
        this.modified = resource.getModified();
    }

    public IntegrationResourceDTO(final Map<String, String> prefLabel,
                                  final Map<String, String> description,
                                  final String uri,
                                  final String status,
                                  final String type,
                                  final String modified) {
        this.prefLabel = prefLabel;
        this.description = description;
        this.uri = uri;
        this.status = status;
        this.type = type;
        this.modified = modified;
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

    @Override
    public String toString() {
        return "IntegrationResourceDTO{" +
            "prefLabel=" + prefLabel +
            ", description=" + description +
            ", uri='" + uri + '\'' +
            ", container='" + container + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            '}';
    }
}
