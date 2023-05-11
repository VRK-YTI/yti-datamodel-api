package fi.vm.yti.datamodel.api.v2.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassInfoDTO extends ResourceInfoBaseDTO {

    private Map<String, String> label;
    private String editorialNote;
    private Status status;
    private Set<String> equivalentClass;
    private Set<String> subClassOf;
    private ConceptDTO subject;
    private String identifier;
    private Map<String, String> note;
    private String uri;
    private Set<OrganizationDTO> contributor;
    private String contact;
    private List<SimpleResourceDTO> attribute = new ArrayList<>();
    private List<SimpleResourceDTO> association = new ArrayList<>();
    private String targetClass;

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

    public ConceptDTO getSubject() {
        return subject;
    }

    public void setSubject(ConceptDTO subject) {
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Set<OrganizationDTO> getContributor() {
        return contributor;
    }

    public void setContributor(Set<OrganizationDTO> contributor) {
        this.contributor = contributor;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<SimpleResourceDTO> getAttribute() {
        return attribute;
    }

    public void setAttribute(List<SimpleResourceDTO> attribute) {
        this.attribute = attribute;
    }

    public List<SimpleResourceDTO> getAssociation() {
        return association;
    }

    public void setAssociation(List<SimpleResourceDTO> association) {
        this.association = association;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }
}
