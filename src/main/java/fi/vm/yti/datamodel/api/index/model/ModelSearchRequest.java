package fi.vm.yti.datamodel.api.index.model;

public class ModelSearchRequest {

    private String query;

    private boolean searchResources;

    private String sortLang;

    private Integer pageSize;

    private Integer pageFrom;

    public ModelSearchRequest() {
    }

    public ModelSearchRequest(final String query,
                              final boolean searchResources,
                              final String sortLang,
                              final Integer pageSize,
                              final Integer pageFrom) {
        this.query = query;
        this.searchResources = searchResources;
        this.sortLang = sortLang;
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