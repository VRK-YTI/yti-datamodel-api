package fi.vm.yti.datamodel.api.index.model;

    import java.util.List;
    import java.util.Map;

public class ModelSearchResponse {

    private long totalHitCount;
    private Integer resultStart;
    private List<IndexModelDTO> models;
    private Map<String, List<DeepSearchHitListDTO<?>>> deepHits;

    public ModelSearchResponse() {}

    public ModelSearchResponse(final long totalHitCount,
                               final Integer resultStart,
                               final List<IndexModelDTO> models,
                               final Map<String, List<DeepSearchHitListDTO<?>>> deepHits) {
        this.totalHitCount = totalHitCount;
        this.resultStart = resultStart;
        this.models = models;
        this.deepHits = deepHits;
    }

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(final long totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public Integer getResultStart() {
        return resultStart;
    }

    public void setResultStart(final Integer resultStart) {
        this.resultStart = resultStart;
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
            ", resultStart=" + resultStart +
            ", models=" + models +
            ", deepHits=" + deepHits +
            '}';
    }
}