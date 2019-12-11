package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

import fi.vm.yti.datamodel.api.model.AbstractPredicate;

public class IndexPredicateDTO extends IndexResourceDTO {

    private String id;
    private String isDefinedBy;
    private String status;
    private String statusModified;
    private String modified;
    private String type;
    private Map<String, String> label;
    private Map<String, String> comment;

    public IndexPredicateDTO() {
    }

    public IndexPredicateDTO(final String id,
                             final String isDefinedBy,
                             final String status,
                             final String statusModified,
                             final String modified,
                             final String type,
                             final Map<String, String> label,
                             final Map<String, String> comment) {
        this.id = id;
        this.isDefinedBy = isDefinedBy;
        this.status = status;
        this.statusModified = statusModified;
        this.modified = modified;
        this.type = type;
        this.label = label;
        this.comment = comment;
    }

    public IndexPredicateDTO(AbstractPredicate predicate) {
        this.id = predicate.getId();
        this.isDefinedBy = predicate.getModelId();
        this.status = predicate.getStatus();
        this.statusModified = predicate.getStatusModified();
        this.modified = predicate.getModified();
        this.type = predicate.getType();
        this.label = predicate.getLabel();
        this.comment = predicate.getComment();
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

    @Override
    public String getStatusModified() {
        return statusModified;
    }

    @Override
    public void setStatusModified(final String statusModified) {
        this.statusModified = statusModified;
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
        return "IndexPredicateDTO{" +
            "id='" + id + '\'' +
            ", isDefinedBy='" + isDefinedBy + '\'' +
            ", status='" + status + '\'' +
            ", statusModified='" + statusModified + '\'' +
            ", modified='" + modified + '\'' +
            ", type='" + type + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            '}';
    }
}


