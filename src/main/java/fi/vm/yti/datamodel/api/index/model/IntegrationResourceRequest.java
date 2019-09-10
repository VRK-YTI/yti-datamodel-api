package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;
import java.util.Set;

public class IntegrationResourceRequest {

    private String searchTerm;
    private String language;
    private String container;
    private String status;
    private String modified;
    private Set<String> filter;
    private Integer pageSize;
    private Integer pageFrom;

    public IntegrationResourceRequest(){}

    public IntegrationResourceRequest(final String searchTerm,
                                      final String language,
                                      final String container,
                                      final String status,
                                      final String modified,
                                      final Set<String> filter,
                                      final Integer pageSize,
                                      final Integer pageFrom) {
        this.searchTerm = searchTerm;
        this.language = language;
        this.container = container;
        this.status = status;
        this.modified = modified;
        this.filter = filter;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(final String container) {
        this.container = container;
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


    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
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
}
