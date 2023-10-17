package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModelMetaData {

    private Map<String, String> label = Map.of();
    private Map<String, String> description = Map.of();
    private Set<UUID> organizations = Set.of();
    private Set<String> groups = Set.of();
    private String contact;
    private Map<String, String> documentation = Map.of();
    private Set<LinkDTO> links = Set.of();

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public Set<UUID> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<UUID> organizations) {
        this.organizations = organizations;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Map<String, String> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Map<String, String> documentation) {
        this.documentation = documentation;
    }

    public Set<LinkDTO> getLinks() {
        return links;
    }

    public void setLinks(Set<LinkDTO> links) {
        this.links = links;
    }
}
