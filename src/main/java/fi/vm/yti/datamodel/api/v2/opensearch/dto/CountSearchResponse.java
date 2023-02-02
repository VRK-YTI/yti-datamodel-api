package fi.vm.yti.datamodel.api.v2.opensearch.dto;

public class CountSearchResponse {

    private long totalHitCount;
    private CountDTO counts;

    public CountSearchResponse() {
        this.totalHitCount = 0;
        this.counts = new CountDTO();
    }

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(long totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public CountDTO getCounts() {
        return counts;
    }

    public void setCounts(CountDTO counts) {
        this.counts = counts;
    }
}

