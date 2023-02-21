package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.Set;

public class ModelSearchRequest extends BaseSearchRequest {

    private String language;

    private Set<String> type;

    private Set<String> includeIncompleteFrom;

    private Set<String> organizations;

    private Set<String> groups;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<String> getType() {
        return type;
    }

    public void setType(Set<String> type) {
        this.type = type;
    }

    public Set<String> getIncludeIncompleteFrom() {
        return includeIncompleteFrom;
    }

    public void setIncludeIncompleteFrom(Set<String> includeIncompleteFrom) {
        this.includeIncompleteFrom = includeIncompleteFrom;
    }

    public Set<String> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<String> organizations) {
        this.organizations = organizations;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
