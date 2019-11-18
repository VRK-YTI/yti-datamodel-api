package fi.vm.yti.datamodel.api.index.model;

import java.util.Date;
import java.util.Set;

public class ResourceSearchRequest {

    private String query;

    private Date after;

    private Date before;

    private String type;

    private String isDefinedBy;

    private Set<String> isDefinedBySet;

    private Set<String> status;

    private String sortLang;

    private String sortField;

    private String sortOrder;

    private Integer pageSize;

    private Integer pageFrom;

    private Set<String> filter;

    public ResourceSearchRequest() {
    }

    public ResourceSearchRequest(IntegrationResourceRequest request) {
        this.isDefinedBySet = request.getContainer();
        this.query = request.getSearchTerm();
        this.status = request.getStatus();
        this.type = request.getType();
        this.after = request.getAfter();
        this.before = request.getBefore();
        this.sortLang = request.getLanguage();
        this.filter = request.getFilter();
        this.pageFrom = request.getPageFrom();
        this.pageSize = request.getPageSize();
    }

    public ResourceSearchRequest(final String query,
                                 final String isDefinedBy,
                                 final Set<String> status,
                                 final Date after,
                                 final Date before,
                                 final String sortLang,
                                 final Integer pageSize,
                                 final Integer pageFrom) {
        this.query = query;
        this.isDefinedBy = isDefinedBy;
        this.status = status;
        this.after = after;
        this.before = before;
        this.sortLang = sortLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public ResourceSearchRequest(final String query,
                                 final String type,
                                 final String isDefinedBy,
                                 final Set<String> status,
                                 final String sortLang,
                                 final String sortField,
                                 final String sortOrder,
                                 final Integer pageSize,
                                 final Integer pageFrom) {
        this.query = query;
        this.type = type;
        this.isDefinedBy = isDefinedBy;
        this.status = status;
        this.sortLang = sortLang;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
    }

    public void setIsDefinedBy(final String isDefinedBy) {
        this.isDefinedBy = isDefinedBy;
    }

    public Set<String> getIsDefinedBySet() {
        return isDefinedBySet;
    }

    public void setIsDefinedBySet(final Set<String> isDefinedBySet) {
        this.isDefinedBySet = isDefinedBySet;
    }

    public Set<String> getStatus() {
        return status;
    }

    public void setStatus(final Set<String> status) {
        this.status = status;
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

    public String getSortLang() {
        return sortLang;
    }

    public void setSortLang(final String sortLang) {
        this.sortLang = sortLang;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(final String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(final String sortOrder) {
        this.sortOrder = sortOrder;
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

    @Override
    public String toString() {
        return "ResourceSearchRequest{" +
            "query='" + query + '\'' +
            ", after=" + after +
            ", before=" + before +
            ", type='" + type + '\'' +
            ", isDefinedBy='" + isDefinedBy + '\'' +
            ", isDefinedBySet=" + isDefinedBySet +
            ", status=" + status +
            ", sortLang='" + sortLang + '\'' +
            ", sortField='" + sortField + '\'' +
            ", sortOrder='" + sortOrder + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            ", filter=" + filter +
            '}';
    }
}
