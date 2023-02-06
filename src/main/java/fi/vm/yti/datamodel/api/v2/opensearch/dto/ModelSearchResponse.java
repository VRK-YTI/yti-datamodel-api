package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.List;

public class ModelSearchResponse {
    private long totalHitCount;
    private Integer pageSize;
    private Integer pageFrom;
    private List<IndexModelDTO> models;

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(long totalHitCount) {
        this.totalHitCount = totalHitCount;
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

    public List<IndexModelDTO> getModels() {
        return models;
    }

    public void setModels(List<IndexModelDTO> models) {
        this.models = models;
    }
}
