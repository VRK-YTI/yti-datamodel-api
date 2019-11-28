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
    private String modified;
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

    public IndexModelDTO() {
    }

    public IndexModelDTO(DataModel model) {
        this.id = model.getId();
        this.useContext = model.getUseContext();
        this.modified = model.getModified();
        this.contentModified = model.getModified();
        this.type = model.getType();
        this.prefix = model.getPrefix();
        this.namespace = model.getNamespace();
        this.status = model.getStatus();
        this.label = model.getLabel();
        this.comment = model.getComment();
        this.contributor = model.getOrganizations();
        this.isPartOf = model.getDomains();
    }

    public IndexModelDTO(final String id,
                         final String useContext,
                         final String status,
                         final String modified,
                         final String contentModified,
                         final String type,
                         final String prefix,
                         final String namespace,
                         final Map<String, String> label,
                         final Map<String, String> comment,
                         final List<UUID> contributor,
                         final List<String> isPartOf) {
        this.id = id;
        this.useContext = useContext;
        this.status = status;
        this.modified = modified;
        this.contentModified = contentModified;
        this.type = type;
        this.prefix = prefix;
        this.namespace = namespace;
        this.label = label;
        this.comment = comment;
        this.contributor = contributor;
        this.isPartOf = isPartOf;
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

    @Override
    public String toString() {
        return "IndexModelDTO{" +
            "id='" + id + '\'' +
            ", useContext='" + useContext + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            ", contentModified='" + contentModified + '\'' +
            ", type='" + type + '\'' +
            ", prefix='" + prefix + '\'' +
            ", namespace='" + namespace + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            ", contributor=" + contributor +
            ", isPartOf=" + isPartOf +
            '}';
    }
}
