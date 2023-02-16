package fi.vm.yti.datamodel.api.v2.opensearch.index;

import java.util.Map;

public class IndexClass extends IndexBase {
    private String isDefinedBy;
    private String contentModified;
    private Map<String, String> note;
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

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
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
