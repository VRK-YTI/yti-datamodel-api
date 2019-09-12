package fi.vm.yti.datamodel.api.index.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class IntegrationContainerRequest {

    private String searchTerm;
    private String language;
    private Set<String> status;
    private Date after;
    private Set<String> filter;
    private Integer pageSize;
    private Integer pageFrom;

    public IntegrationContainerRequest(){}

    public IntegrationContainerRequest(final String searchTerm,
                                       final String language,
                                       final Set<String> status,
                                       final Date after,
                                       final Set<String> filter,
                                       final Integer pageSize,
                                       final Integer pageFrom) {
        this.searchTerm = searchTerm;
        this.language = language;
        this.status = status;
        this.after = after;
        this.filter = filter;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(final String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
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

    public Set<String> getFilter() {
        return filter;
    }

    public void setFilter(final Set<String> filter) {
        this.filter = filter;
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

    @Override
    public String toString() {
        return "IntegrationContainerRequest{" +
            "searchTerm='" + searchTerm + '\'' +
            ", language='" + language + '\'' +
            ", status='" + status + '\'' +
            ", after='" + after + '\'' +
            ", filter=" + filter +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            '}';
    }
}
