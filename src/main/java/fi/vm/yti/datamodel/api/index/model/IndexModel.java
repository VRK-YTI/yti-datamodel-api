package fi.vm.yti.datamodel.api.index.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.vm.yti.datamodel.api.model.DataModel;

public class IndexModel {

    private String id;
    private String useContext;
    private String status;
    private String modified;
    private String type;
    private Map<String, String> label;
    private Map<String, String> comment;
    private List<UUID> contributors;
    private List<String> domains;

    public IndexModel() {}

    public IndexModel(final String id,
                      final String useContext,
                      final String modified,
                      final String type,
                      final String status,
                      final Map<String, String> label,
                      final Map<String, String> comment,
                      final List<UUID> contributors,
                      final List<String> domains) {
        this.id = id;
        this.useContext = useContext;
        this.modified = modified;
        this.type = type;
        this.status = status;
        this.label = label;
        this.comment = comment;
        this.contributors = contributors;
        this.domains = domains;
    }

    public IndexModel(DataModel model) {
        this.id = model.getId();
        this.useContext = model.getUseContext();
        this.modified = model.getModified();
        this.type = model.getType();
        this.status = model.getStatus();
        this.label = model.getLabel();
        this.comment = model.getComment();
        this.contributors = model.getOrganizations();
        this.domains = model.getDomains();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public List<UUID> getContributors() {
        return contributors;
    }

    public void setContributors(final List<UUID> contributors) {
        this.contributors = contributors;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUseContext() {
        return useContext;
    }

    public void setUseContext(final String useContext) {
        this.useContext = useContext;
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

    public void setComment(final Map<String, String> comment) { this.comment = comment; }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(final List<String> domains) {
        this.domains = domains;
    }

    @Override
    public String toString() {
        return "IndexModel{" +
            "id='" + id + '\'' +
            ", useContext='" + useContext + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            ", type='" + type + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            ", contributors=" + contributors +
            ", domains=" + domains +
            '}';
    }
}
