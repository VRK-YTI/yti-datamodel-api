package fi.vm.yti.datamodel.api.index.model;

import java.util.List;
import java.util.Map;

public class ResourceSearchResponse {

    private long totalHitCount;
    private Integer resultStart;
    private List<IndexResourceDTO> resources;

    public ResourceSearchResponse() {}

    public ResourceSearchResponse(final long totalHitCount,
                                  final Integer resultStart,
                                  final List<IndexResourceDTO> resources) {
        this.totalHitCount = totalHitCount;
        this.resultStart = resultStart;
        this.resources = resources;
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
            ", resultStart=" + resultStart +
            ", resources=" + resources +
            '}';
    }
}