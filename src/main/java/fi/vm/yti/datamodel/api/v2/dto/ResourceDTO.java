package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.Set;

public class ResourceDTO {

    private ResourceType type;
    private Map<String, String> label;
    private String editorialNote;
    private Status status;
    private Set<String> subResourceOf;
    private Set<String> equivalentResource;
    private String subject;
    private String identifier;
    private Map<String, String> note;
    private String domain;
    private String range;

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
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

    public Set<String> getSubResourceOf() {
        return subResourceOf;
    }

    public void setSubResourceOf(Set<String> subResourceOf) {
        this.subResourceOf = subResourceOf;
    }

    public Set<String> getEquivalentResource() {
        return equivalentResource;
    }

    public void setEquivalentResource(Set<String> equivalentResource) {
        this.equivalentResource = equivalentResource;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
    }

    public String getEditorialNote() {
        return editorialNote;
    }

    public void setEditorialNote(String editorialNote) {
        this.editorialNote = editorialNote;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
