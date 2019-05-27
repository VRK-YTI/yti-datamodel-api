package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

import fi.vm.yti.datamodel.api.model.AbstractClass;

public class IndexClassDTO extends IndexResourceDTO {

    private String id;
    private String isDefinedBy;
    private String status;
    private String modified;
    private String type;
    private Map<String, String> label;
    private Map<String, String> comment;

    public IndexClassDTO() {
    }

    public IndexClassDTO(final String id,
                         final String isDefinedBy,
                         final String status,
                         final String modified,
                         final String type,
                         final Map<String, String> label,
                         final Map<String, String> comment) {
        this.id = id;
        this.isDefinedBy = isDefinedBy;
        this.status = status;
        this.modified = modified;
        this.type = type;
        this.label = label;
        this.comment = comment;
    }

    public IndexClassDTO(AbstractClass classResource) {
        this.id = classResource.getId();
        this.isDefinedBy = classResource.getModelId();
        this.status = classResource.getStatus();
        this.modified = classResource.getModified();
        this.type = classResource.getType();
        this.label = classResource.getLabel();
        this.comment = classResource.getComment();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
    }

    public void setIsDefinedBy(final String isDefinedBy) {
        this.isDefinedBy = isDefinedBy;
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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(final Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(final Map<String, String> comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "IndexClassDTO{" +
            "id='" + id + '\'' +
            ", isDefinedBy='" + isDefinedBy + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            ", type='" + type + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            '}';
    }
}
