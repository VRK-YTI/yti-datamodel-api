package fi.vm.yti.datamodel.api.index.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class ModelSearchRequest {

    private String query;

    private boolean searchResources;

    private String prefLang;

    private Integer pageSize;

    private Integer pageFrom;

    public ModelSearchRequest() {}

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final String prefLang,
                              final Integer pageSize,
                              final Integer pageFrom) {
        this.query = query;
        this.searchResources = searchResources;
        this.prefLang = prefLang;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
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

    public String getPrefLang() {
        return prefLang;
    }

    public void setPrefLang(final String prefLang) {
        this.prefLang = prefLang;
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
        return "ModelSearchRequest{" +
            "query='" + query + '\'' +
            ", searchResources=" + searchResources +
            ", prefLang='" + prefLang + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            '}';
    }
}