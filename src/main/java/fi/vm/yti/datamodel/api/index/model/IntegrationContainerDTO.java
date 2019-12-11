package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

public class IntegrationContainerDTO {

    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private String uri;
    private String status;
    private String type;
    private String modified;
    private String contentModified;

    public IntegrationContainerDTO(IndexModelDTO model) {
        this.prefLabel = model.getLabel();
        this.description = model.getComment();
        this.uri = model.getId();
        this.status = model.getStatus();
        this.type = model.getType();
        this.modified = model.getModified();
        this.contentModified = model.getContentModified();
    }

    public IntegrationContainerDTO(final Map<String, String> prefLabel,
                                   final Map<String, String> description,
                                   final String uri,
                                   final String status,
                                   final String type,
                                   final String modified,
                                   final String contentModified) {
        this.prefLabel = prefLabel;
        this.description = description;
        this.uri = uri;
        this.status = status;
        this.type = type;
        this.modified = modified;
        this.contentModified = contentModified;
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

    public String getContentModified() {
        return contentModified;
    }

    public void setContentModified(final String contentModified) {
        this.contentModified = contentModified;
    }

    @Override
    public String toString() {
        return "IntegrationContainerDTO{" +
            "prefLabel=" + prefLabel +
            ", description=" + description +
            ", uri='" + uri + '\'' +
            ", status='" + status + '\'' +
            ", type='" + type + '\'' +
            ", modified='" + modified + '\'' +
            ", contentModified='" + contentModified + '\'' +
            '}';
    }
}
