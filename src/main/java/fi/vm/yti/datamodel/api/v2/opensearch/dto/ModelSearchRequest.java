package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.datamodel.api.v2.dto.ModelType;

import java.util.Set;
import java.util.UUID;

public class ModelSearchRequest extends BaseSearchRequest {

    private String language;

    private Set<ModelType> type;

    private Set<UUID> includeIncompleteFrom;

    private Set<UUID> organizations;

    private Set<String> groups;

    private boolean searchResources;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<ModelType> getType() {
        return type;
    }

    public void setType(Set<ModelType> type) {
        this.type = type;
    }

    public Set<UUID> getIncludeIncompleteFrom() {
        return includeIncompleteFrom;
    }

    public void setIncludeIncompleteFrom(Set<UUID> includeIncompleteFrom) {
        this.includeIncompleteFrom = includeIncompleteFrom;
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

    public boolean isSearchResources() {
        return searchResources;
    }

    public void setSearchResources(boolean searchResources) {
        this.searchResources = searchResources;
    }
}
