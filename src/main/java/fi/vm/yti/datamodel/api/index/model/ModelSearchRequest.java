package fi.vm.yti.datamodel.api.index.model;

import java.util.Date;
import java.util.Set;

public class ModelSearchRequest {

    private String query;

    private boolean searchResources;

    private Date after;

    private Date before;

    private String sortLang;

    private Set<String> status;

    private String type;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> filter;

    private Boolean includeIncomplete;

    private Set<String> includeIncompleteFrom;

    public ModelSearchRequest() {
    }

    public ModelSearchRequest(IntegrationContainerRequest request) {
        this.query = request.getSearchTerm();
        this.status = request.getStatus();
        this.type = request.getType();
        this.after = request.getAfter();
        this.before = request.getBefore();
        this.sortLang = request.getLanguage();
        this.filter = request.getFilter();
        this.pageFrom = request.getPageFrom();
        this.pageSize = request.getPageSize();
        this.includeIncomplete = request.getIncludeIncomplete();
        this.includeIncompleteFrom = request.getIncludeIncompleteFrom();
    }

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final Set<String> status,
                              final String type,
                              final Date after,
                              final Date before,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom,
                              final Boolean includeIncomplete,
                              final Set<String> includeIncompleteFrom) {
        this.query = query;
        this.searchResources = searchResources;
        this.status = status;
        this.type = type;
        this.after = after;
        this.before = before;
        this.sortLang = sortLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
        this.includeIncomplete = includeIncomplete;
        this.includeIncompleteFrom = includeIncompleteFrom;
    }

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final Set<String> status,
                              final String type,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom,
                              final Set<String> filter) {
        this.query = query;
        this.searchResources = searchResources;
        this.status = status;
        this.type = type;
        this.sortLang = sortLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
        this.filter = filter;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
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

    @Override
    public String toString() {
        return "ModelSearchRequest{" +
            "query='" + query + '\'' +
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
            '}';
    }
}
