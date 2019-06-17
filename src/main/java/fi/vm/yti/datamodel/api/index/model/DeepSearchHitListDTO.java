package fi.vm.yti.datamodel.api.index.model;

import java.util.List;

public abstract class DeepSearchHitListDTO<T> {

    private String type;
    private long totalHitCount;
    private List<T> topHits;

    protected DeepSearchHitListDTO() {
    }

    protected DeepSearchHitListDTO(String type,
                                   long totalHitCount,
                                   List<T> topHits) {
        this.type = type;
        this.totalHitCount = totalHitCount;
        this.topHits = topHits;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(final long totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public List<T> getTopHits() {
        return topHits;
    }

    public void setTopHits(final List<T> topHits) {
        this.topHits = topHits;
    }
}
