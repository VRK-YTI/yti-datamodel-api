package fi.vm.yti.datamodel.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

public class NodeShapeInfoDTO extends ResourceInfoBaseDTO {
    private String targetClass;
    private String targetNode;
    private List<SimplePropertyShapeDTO> attribute = new ArrayList<>();
    private List<SimplePropertyShapeDTO> association = new ArrayList<>();

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

    public List<SimplePropertyShapeDTO> getAttribute() {
        return attribute;
    }

    public void setAttribute(List<SimplePropertyShapeDTO> attribute) {
        this.attribute = attribute;
    }

    public List<SimplePropertyShapeDTO> getAssociation() {
        return association;
    }

    public void setAssociation(List<SimplePropertyShapeDTO> association) {
        this.association = association;
    }
}
