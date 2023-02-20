package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.datamodel.api.v2.dto.Status;

import java.util.Set;

public abstract class BaseSearchRequest {

    private String query;

    private String sortLang;

    private Set<Status> status;

    private Integer pageSize;

    private Integer pageFrom;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSortLang() {
        return sortLang;
    }

    public void setSortLang(String sortLang) {
        this.sortLang = sortLang;
    }

    public Set<Status> getStatus() {
        return status;
    }

    public void setStatus(Set<Status> status) {
        this.status = status;
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
}
