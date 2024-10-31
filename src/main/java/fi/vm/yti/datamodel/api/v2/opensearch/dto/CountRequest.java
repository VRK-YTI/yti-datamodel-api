package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;

import java.util.Set;
import java.util.UUID;

public class CountRequest {
    private String query;
    private String language;
    private Set<GraphType> type;
    private Set<Status> status;
    private Set<UUID> organizations;
    private Set<String> groups;
    private Set<UUID> includeDraftFrom;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

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

    public Set<Status> getStatus() {
        return status;
    }

    public void setStatus(Set<Status> status) {
        this.status = status;
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

    public Set<UUID> getIncludeDraftFrom() {
        return includeDraftFrom;
    }

    public void setIncludeDraftFrom(Set<UUID> includeDraftFrom) {
        this.includeDraftFrom = includeDraftFrom;
    }
}
