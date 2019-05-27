package fi.vm.yti.datamodel.api.index.model;

public class ResourceSearchRequest {

    private String query;

    private String type;

    private String isDefinedBy;

    private String prefLang;

    private Integer pageSize;

    private Integer pageFrom;

    public ResourceSearchRequest() {}

    public ResourceSearchRequest(final String query,
                                 final String type,
                                 final String isDefinedBy,
                                 final String prefLang,
                                 final Integer pageSize,
                                 final Integer pageFrom) {
        this.query = query;
        this.type = type;
        this.isDefinedBy = isDefinedBy;
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
        return "ResourceSearchRequest{" +
            "query='" + query + '\'' +
            ", type='" + type + '\'' +
            ", isDefinedBy='" + isDefinedBy + '\'' +
            ", prefLang='" + prefLang + '\'' +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            '}';
    }
}