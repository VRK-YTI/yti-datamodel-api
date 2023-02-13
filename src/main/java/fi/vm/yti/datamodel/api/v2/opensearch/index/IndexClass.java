package fi.vm.yti.datamodel.api.v2.opensearch.index;

public class IndexClass extends IndexBase {
    private String isDefinedBy;
    private String contentModified;
    private String comment;
    private String identifier;
    private String namespace;

    public void setIsDefinedBy(String isDefinedBy) {
        this.isDefinedBy = isDefinedBy;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
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
