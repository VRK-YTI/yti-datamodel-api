package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

public class IntegrationContainerDTO {

    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private String uri;
    private String status;
    private String modified;

    public IntegrationContainerDTO(IndexModelDTO model) {
        this.prefLabel = model.getLabel();
        this.description = model.getComment();
        this.uri = model.getId();
        this.status = model.getStatus();
        this.modified = model.getModified();
    }

    public IntegrationContainerDTO(final Map<String, String> prefLabel,
                                   final Map<String, String> description,
                                   final String uri,
                                   final String status,
                                   final String modified) {
        this.prefLabel = prefLabel;
        this.description = description;
        this.uri = uri;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return "IntegrationContainerDTO{" +
            "prefLabel=" + prefLabel +
            ", description=" + description +
            ", uri='" + uri + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            '}';
    }
}
