package fi.vm.yti.datamodel.api.v2.opensearch.index;

import java.util.Map;

public class IndexClass {

    private String id;
    private String isDefinedBy;
    private String status;
    private String modified;
    private String created;
    private String contentModified;
    private String comment;
    private Map<String, String> label;
    private String identifier;
    private String namespace;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIsDefinedBy(String isDefinedBy) {
        this.isDefinedBy = isDefinedBy;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getContentModified() {
        return contentModified;
    }

    public void setContentModified(String contentModified) {
        this.contentModified = contentModified;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
