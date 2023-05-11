package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassDTO {

    private Map<String, String> label;
    private String editorialNote;
    private Status status;
    private Set<String> equivalentClass;
    private Set<String> subClassOf;
    private String subject;
    private String identifier;
    private Map<String, String> note;
    private String targetClass; //Only allowed in Application profile classes
    private String targetNode;
    private List<String> properties;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getEditorialNote() {
        return editorialNote;
    }

    public void setEditorialNote(String editorialNote) {
        this.editorialNote = editorialNote;
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

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
