package fi.vm.yti.datamodel.api.index.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import fi.vm.yti.datamodel.api.model.DataModel;

public class IndexModelDTO {

    private String id;
    private String useContext;
    private String status;
    private String statusModified;
    private String modified;
    private String created;
    private String contentModified;
    private String type;
    private String prefix;
    private String namespace;
    private Map<String, String> label;
    private Map<String, String> comment;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<UUID> contributor;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> isPartOf;
    private List<String> language;

    public IndexModelDTO() {
    }

    public IndexModelDTO(DataModel model) {
        this.id = model.getId();
        this.useContext = model.getUseContext();
        this.modified = model.getModified();
        this.created = model.getCreated();
        this.contentModified = model.getContentModified();
        this.type = model.getType();
        this.prefix = model.getPrefix();
        this.namespace = model.getNamespace();
        this.status = model.getStatus();
        this.statusModified = model.getStatusModified();
        this.label = model.getLabel();
        this.comment = model.getComment();
        this.contributor = model.getOrganizations();
        this.isPartOf = model.getDomains();
        this.language = model.getLanguages();
    }

    public IndexModelDTO(final String id,
                         final String useContext,
                         final String status,
                         final String statusModified,
                         final String modified,
                         final String created,
                         final String contentModified,
                         final String type,
                         final String prefix,
                         final String namespace,
                         final Map<String, String> label,
                         final Map<String, String> comment,
                         final List<UUID> contributor,
                         final List<String> isPartOf,
                         final List<String> language) {
        this.id = id;
        this.useContext = useContext;
        this.status = status;
        this.statusModified = statusModified;
        this.modified = modified;
        this.created = created;
        this.contentModified = contentModified;
        this.type = type;
        this.prefix = prefix;
        this.namespace = namespace;
        this.label = label;
        this.comment = comment;
        this.contributor = contributor;
        this.isPartOf = isPartOf;
        this.language = language;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getStatusModified() {
        return statusModified;
    }

    public void setStatusModified(final String statusModified) {
        this.statusModified = statusModified;
    }

    public String getModified() {
        return modified;
    }

    public String getCreated() { return created; }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getContentModified() {
        return contentModified;
    }

    public void setContentModified(final String contentModified) {
        this.contentModified = contentModified;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
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

    public List<UUID> getContributor() {
        return contributor;
    }

    public void setContributor(final List<UUID> contributor) {
        this.contributor = contributor;
    }

    public List<String> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(final List<String> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(final List<String> language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "IndexModelDTO{" +
            "id='" + id + '\'' +
            ", useContext='" + useContext + '\'' +
            ", status='" + status + '\'' +
            ", statusModified='" + statusModified + '\'' +
            ", modified='" + modified + '\'' +
            ", created='" + created + '\'' +
            ", contentModified='" + contentModified + '\'' +
            ", type='" + type + '\'' +
            ", prefix='" + prefix + '\'' +
            ", namespace='" + namespace + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            ", contributor=" + contributor +
            ", isPartOf=" + isPartOf +
            ", language=" + language +
            '}';
    }
}
