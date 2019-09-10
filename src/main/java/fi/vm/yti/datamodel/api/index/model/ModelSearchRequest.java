package fi.vm.yti.datamodel.api.index.model;

import java.util.Set;

public class ModelSearchRequest {

    private String query;

    private boolean searchResources;

    private String sortLang;

    private String status;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> filter;

    public ModelSearchRequest() {
    }

    public ModelSearchRequest(IntegrationResourceRequest request) {
        this.query = request.getSearchTerm();
        this.status = request.getStatus();
        this.sortLang = request.getLanguage();
        this.filter = request.getFilter();
        this.pageFrom = request.getPageFrom();
        this.pageSize = request.getPageSize();
    }


    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final String status,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom) {
        this.query = query;
        this.searchResources = searchResources;
        this.status = status;
        this.sortLang = sortLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final String status,
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ModelSearchRequest{" +
            "query='" + query + '\'' +
            ", searchResources=" + searchResources +
            ", sortLang='" + sortLang + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            '}';
    }
}
