package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;

public class IndexOrganization {

    private String id;
    private Map<String,String> label;
    private Map<String,String> comment;

    public IndexOrganization() {}

    public IndexOrganization(final String id,
                             final Map<String, String> label,
                             final Map<String, String> comment) {
        this.id = id;
        this.label = label;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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
        return "Organization{" +
            "id='" + id + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            '}';
    }
}
