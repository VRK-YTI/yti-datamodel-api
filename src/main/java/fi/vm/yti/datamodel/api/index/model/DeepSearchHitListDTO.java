package fi.vm.yti.datamodel.api.index.model;

import java.util.List;

public abstract class DeepSearchHitListDTO<T> {

    private long totalHitCount;

    private List<T> topHits;

    protected DeepSearchHitListDTO() {
    }

    protected DeepSearchHitListDTO(long totalHitCount,
                                   List<T> topHits) {

        this.totalHitCount = totalHitCount;
        this.topHits = topHits;

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