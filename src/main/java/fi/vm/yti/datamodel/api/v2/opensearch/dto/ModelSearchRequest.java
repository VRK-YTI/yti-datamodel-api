package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.opensearch.BaseSearchRequest;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class ModelSearchRequest extends BaseSearchRequest {

    private String language;

    private Set<GraphType> type;

    private Set<UUID> includeDraftFrom;

    private Set<UUID> organizations;

    private Set<String> groups;

    private boolean searchResources;

    private Set<String> additionalModelIds;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<GraphType> getType() {
        return type;
    }

    public void setType(Set<GraphType> type) {
        this.type = type;
    }

    public Set<UUID> getIncludeDraftFrom() {
        return includeDraftFrom;
    }

    public void setIncludeDraftFrom(Set<UUID> includeDraftFrom) {
        this.includeDraftFrom = includeDraftFrom;
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

    public Collection<String> getAdditionalModelIds() {
        return additionalModelIds;
    }

    public void setAdditionalModelIds(Set<String> additionalModelIds) {
        this.additionalModelIds = additionalModelIds;
    }
}
