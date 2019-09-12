package fi.vm.yti.datamodel.api.index.model;

import java.util.Date;
import java.util.Set;

public class ModelSearchRequest {

    private String query;

    private boolean searchResources;

    private Date after;

    private String sortLang;

    private Set<String> status;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> filter;

    public ModelSearchRequest() {
    }

    public ModelSearchRequest(IntegrationContainerRequest request) {
        this.query = request.getSearchTerm();
        this.status = request.getStatus();
        this.after = request.getAfter();
        this.sortLang = request.getLanguage();
        this.filter = request.getFilter();
        this.pageFrom = request.getPageFrom();
        this.pageSize = request.getPageSize();
    }


    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final Set<String> status,
                              final Date after,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom) {
        this.query = query;
        this.searchResources = searchResources;
        this.status = status;
        this.after = after;
        this.sortLang = sortLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final Set<String> status,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom,
                              final Set<String> filter) {
        this.query = query;
        this.searchResources = searchResources;
        this.status = status;
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

    @Override
    public String toString() {
        return "ModelSearchRequest{" +
            "query='" + query + '\'' +
            ", searchResources=" + searchResources +
            ", after='" + after + '\'' +
            ", sortLang='" + sortLang + '\'' +
            ", status='" + status + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            ", filter=" + filter +
            '}';
    }
}
