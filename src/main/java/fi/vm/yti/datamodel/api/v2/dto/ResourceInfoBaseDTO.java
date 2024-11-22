package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.enums.Status;

import java.util.Map;
import java.util.Set;

public class ResourceInfoBaseDTO extends ResourceCommonInfoDTO {
    private String editorialNote;
    private Status status;
    private ConceptDTO subject;
    private String identifier;
    private Map<String, String> note;
    private String curie;
    private Set<OrganizationDTO> contributor;
    private String contact;

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

    public String getCurie() {
        return curie;
    }

    public void setCurie(String curie) {
        this.curie = curie;
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

}
