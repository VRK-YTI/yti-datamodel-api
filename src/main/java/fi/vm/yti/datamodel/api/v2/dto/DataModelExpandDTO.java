package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;
import java.util.Set;

public class DataModelExpandDTO {
    private ModelType type;
    private String prefix;
    private Status status;
    private Map<String, String> label = Map.of();
    private Map<String, String> description = Map.of();
    private Set<String> languages = Set.of();
    private Set<OrganizationDTO> organizations = Set.of();
    private Set<ServiceCategoryDTO> groups = Set.of();
    private Set<String> internalNamespaces = Set.of();
    private Set<ExternalNamespaceDTO> externalNamespaces = Set.of();
    private Set<TerminologyDTO> terminologies = Set.of();
    private String modified;
    private String created;

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public Set<OrganizationDTO> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<OrganizationDTO> organizations) {
        this.organizations = organizations;
    }

    public Set<ServiceCategoryDTO> getGroups() {
        return groups;
    }

    public void setGroups(Set<ServiceCategoryDTO> groups) {
        this.groups = groups;
    }

    public Set<String> getInternalNamespaces() {
        return internalNamespaces;
    }

    public void setInternalNamespaces(Set<String> internalNamespaces) {
        this.internalNamespaces = internalNamespaces;
    }

    public Set<ExternalNamespaceDTO> getExternalNamespaces() {
        return externalNamespaces;
    }

    public void setExternalNamespaces(Set<ExternalNamespaceDTO> externalNamespaces) {
        this.externalNamespaces = externalNamespaces;
    }

    public Set<TerminologyDTO> getTerminologies() {
        return terminologies;
    }

    public void setTerminologies(Set<TerminologyDTO> terminologies) {
        this.terminologies = terminologies;
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
}
