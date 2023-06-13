package fi.vm.yti.datamodel.api.v2.dto;

import java.util.List;

public class NodeShapeDTO extends BaseDTO {
    private String targetClass;
    private String targetNode;
    private List<String> properties;

    public String getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }
}
