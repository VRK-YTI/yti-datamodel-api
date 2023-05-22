package fi.vm.yti.datamodel.api.v2.dto;

public class NodeShapeInfoDTO extends ResourceInfoBaseDTO {
    private String targetClass;
    private String targetNode;

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }
}
