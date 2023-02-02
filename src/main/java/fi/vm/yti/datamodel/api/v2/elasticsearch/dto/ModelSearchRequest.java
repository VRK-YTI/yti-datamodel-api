package fi.vm.yti.datamodel.api.v2.elasticsearch.dto;

import java.util.Set;

public class ModelSearchRequest {

    private String query;

    private String language;

    private String sortLang;

    private Set<String> status;

    private Set<String> type;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> includeIncompleteFrom;

    private Set<String> organizations;

    private Set<String> groups;

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

    public String getSortLang() {
        return sortLang;
    }

    public void setSortLang(String sortLang) {
        this.sortLang = sortLang;
    }

    public Set<String> getStatus() {
        return status;
    }

    public void setStatus(Set<String> status) {
        this.status = status;
    }

    public Set<String> getType() {
        return type;
    }

    public void setType(Set<String> type) {
        this.type = type;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(Integer pageFrom) {
        this.pageFrom = pageFrom;
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