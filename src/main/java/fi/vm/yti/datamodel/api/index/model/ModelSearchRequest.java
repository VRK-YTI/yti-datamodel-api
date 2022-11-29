package fi.vm.yti.datamodel.api.index.model;

import java.util.Date;
import java.util.Set;

public class ModelSearchRequest {

    private Set<String> uri;

    private String query;

    private String language;

    private boolean searchResources;

    private Date after;

    private Date before;

    private String sortLang;

    private Set<String> status;

    private Set<String> type;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> filter;

    private Boolean includeIncomplete;

    private Set<String> includeIncompleteFrom;

    private Set<String> organizations;

    private Set<String> groups;

    public ModelSearchRequest() {
    }

    public ModelSearchRequest(IntegrationContainerRequest request) {
        this.uri = request.getUri();
        this.query = request.getSearchTerm();
        this.status = request.getStatus();
        this.type = request.getType() != null ? Set.of(request.getType()) : Set.of();
        this.after = request.getAfter();
        this.before = request.getBefore();
        this.sortLang = request.getLanguage();
        this.filter = request.getFilter();
        this.pageFrom = request.getPageFrom();
        this.pageSize = request.getPageSize();
        this.includeIncomplete = request.getIncludeIncomplete();
        this.includeIncompleteFrom = request.getIncludeIncompleteFrom();
    }

    public Set<String> getUri() {
        return uri;
    }

    public void setUri(final Set<String> uri) {
        this.uri = uri;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSearchResources() {
        return searchResources;
    }

    public void setSearchResources(final boolean searchResources) {
        this.searchResources = searchResources;
    }

    public String getSortLang() {
        return sortLang;
    }

    public void setSortLang(final String sortLang) {
        this.sortLang = sortLang;
    }

    public Date getAfter() {
        return after;
    }

    public void setAfter(final Date after) {
        this.after = after;
    }

    public Date getBefore() {
        return before;
    }

    public void setBefore(final Date before) {
        this.before = before;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(final Integer pageFrom) {
        this.pageFrom = pageFrom;
    }

    public Set<String> getFilter() {
        return filter;
    }

    public void setFilter(final Set<String> filter) {
        this.filter = filter;
    }

    public Set<String> getStatus() {
        return status;
    }

    public void setStatus(final Set<String> status) {
        this.status = status;
    }

    public Set<String> getType() {
        return type;
    }

    public void setType(final Set<String> type) {
        this.type = type;
    }

    public Boolean getIncludeIncomplete() {
        return includeIncomplete;
    }

    public void setIncludeIncomplete(final Boolean includeIncomplete) {
        this.includeIncomplete = includeIncomplete;
    }

    public Set<String> getIncludeIncompleteFrom() {
        return includeIncompleteFrom;
    }

    public void setIncludeIncompleteFrom(final Set<String> includeIncompleteFrom) {
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

    @Override
    public String toString() {
        return "ModelSearchRequest{" +
            "query='" + query + '\'' +
            ", language=" + language +
            ", searchResources=" + searchResources +
            ", after=" + after +
            ", before=" + before +
            ", sortLang='" + sortLang + '\'' +
            ", status=" + status +
            ", type='" + type + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            ", filter=" + filter +
            ", includeIncomplete=" + includeIncomplete +
            ", includeIncompleteFrom=" + includeIncompleteFrom +
            ", organizations=" + organizations +
            ", groups=" + groups +
            '}';
    }
}
