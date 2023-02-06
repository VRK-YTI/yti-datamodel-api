package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.Set;

public class ClassDTO {

    private Map<String, String> label = Map.of();
    private String comment;
    private Status status;
    private Set<String> equivalentClass = Set.of();
    private Set<String> subClassOf = Set.of();
    private String subject;
    private String identifier;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<String> getEquivalentClass() {
        return equivalentClass;
    }

    public void setEquivalentClass(Set<String> equivalentClass) {
        this.equivalentClass = equivalentClass;
    }

    public Set<String> getSubClassOf() {
        return subClassOf;
    }

    public void setSubClassOf(Set<String> subClassOf) {
        this.subClassOf = subClassOf;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
