package fi.vm.yti.datamodel.api.v2.dto.visualization;

public class VisualizationAttributeNodeDTO extends VisualizationNodeDTO {
    private String dataType;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
