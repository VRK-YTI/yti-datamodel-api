package fi.vm.yti.datamodel.api.index.model;

import java.util.List;

public class ResourceSearchResponse {

    private long totalHitCount;
    private Integer pageSize;
    private Integer pageFrom;
    private List<IndexResourceDTO> resources;

    public ResourceSearchResponse() {
    }

    public ResourceSearchResponse(final long totalHitCount,
                                  final Integer pageSize,
                                  final Integer pageFrom,
                                  final List<IndexResourceDTO> resources) {
        this.totalHitCount = totalHitCount;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
        this.resources = resources;
    }

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(final long totalHitCount) {
        this.totalHitCount = totalHitCount;
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

    public List<IndexResourceDTO> getResources() {
        return resources;
    }

    public void setResources(final List<IndexResourceDTO> resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return "ResourceSearchResponse{" +
            "totalHitCount=" + totalHitCount +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            ", resources=" + resources +
            '}';
    }
}