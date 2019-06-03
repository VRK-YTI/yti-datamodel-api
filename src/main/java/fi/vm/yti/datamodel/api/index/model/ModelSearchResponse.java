package fi.vm.yti.datamodel.api.index.model;

import java.util.List;
import java.util.Map;

public class ModelSearchResponse {

    private long totalHitCount;
    private Integer pageSize;
    private Integer pageFrom;
    private List<IndexModelDTO> models;
    private Map<String, List<DeepSearchHitListDTO<?>>> deepHits;

    public ModelSearchResponse() {
    }

    public ModelSearchResponse(final long totalHitCount,
                               final Integer pageSize,
                               final Integer pageFrom,
                               final List<IndexModelDTO> models,
                               final Map<String, List<DeepSearchHitListDTO<?>>> deepHits) {
        this.totalHitCount = totalHitCount;
        this.pageSize = pageSize;
        this.pageFrom = pageFrom;
        this.models = models;
        this.deepHits = deepHits;
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

    public List<IndexModelDTO> getModels() {
        return models;
    }

    public void setModels(final List<IndexModelDTO> models) {
        this.models = models;
    }

    public Map<String, List<DeepSearchHitListDTO<?>>> getDeepHits() {
        return deepHits;
    }

    public void setDeepHits(final Map<String, List<DeepSearchHitListDTO<?>>> deepHits) {
        this.deepHits = deepHits;
    }

    @Override
    public String toString() {
        return "ModelSearchResponse{" +
            "totalHitCount=" + totalHitCount +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            ", models=" + models +
            ", deepHits=" + deepHits +
            '}';
    }
}