package fi.vm.yti.datamodel.api.v2.dto;

public class PropertyShapeDTO extends BaseDTO {
    private String path;
    private Integer maxCount;
    private Integer minCount;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Integer getMinCount() {
        return minCount;
    }

    public void setMinCount(Integer minCount) {
        this.minCount = minCount;
    }
}
