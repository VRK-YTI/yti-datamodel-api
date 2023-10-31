package fi.vm.yti.datamodel.api.v2.dto.visualization;

public class VisualizationPropertyShapeAssociationDTO extends VisualizationReferenceDTO {
    private Integer minCount;
    private Integer maxCount;

    public Integer getMinCount() {
        return minCount;
    }

    public void setMinCount(Integer minCount) {
        this.minCount = minCount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }
}
