package fi.vm.yti.datamodel.api.index.model;

public class IntegrationMetaDTO {

    private Integer pageSize;
    private Integer from;
    private Integer resultCount;
    private Integer totalResults;
    private String nextPage;

    public IntegrationMetaDTO(final Integer pageSize,
                              final Integer from,
                              final Integer totalResults) {
        this.pageSize = pageSize;
        this.from = from;
        this.totalResults = totalResults;
    }

    public IntegrationMetaDTO(final Integer pageSize,
                              final Integer from,
                              final Integer resultCount,
                              final Integer totalResults,
                              final String nextPage) {
        this.pageSize = pageSize;
        this.from = from;
        this.resultCount = resultCount;
        this.totalResults = totalResults;
        this.nextPage = nextPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(final Integer from) {
        this.from = from;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(final Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(final Integer totalResults) {
        this.totalResults = totalResults;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(final String nextPage) {
        this.nextPage = nextPage;
    }

    @Override
    public String toString() {
        return "IntegrationMetaDTO{" +
            "pageSize=" + pageSize +
            ", from=" + from +
            ", resultCount=" + resultCount +
            ", totalResults=" + totalResults +
            ", nextPage='" + nextPage + '\'' +
            '}';
    }
}
