package fi.vm.yti.datamodel.api.v2.elasticsearch.index;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IndexModel {

    private String id;
    private String status;
    private String statusModified;
    private String modified;
    private String created;
    private String contentModified;
    private String type;
    private String prefix;
    private Map<String, String> label;
    private Map<String, String> comment;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<UUID> contributor;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> isPartOf;
    private List<String> language;
    private Map<String, String> documentation;

    public IndexModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusModified() {
        return statusModified;
    }

    public void setStatusModified(String statusModified) {
        this.statusModified = statusModified;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(Map<String, String> comment) {
        this.comment = comment;
    }

    public List<UUID> getContributor() {
        return contributor;
    }

    public void setContributor(List<UUID> contributor) {
        this.contributor = contributor;
    }

    public List<String> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(List<String> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public Map<String, String> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Map<String, String> documentation) {
        this.documentation = documentation;
    }


}