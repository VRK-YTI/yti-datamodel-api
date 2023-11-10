package fi.vm.yti.datamodel.api.v2.dto.visualization;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class VisualizationClassDTO extends VisualizationNodeDTO {
    private List<VisualizationAttributeDTO> attributes = new ArrayList<>();
    private List<VisualizationReferenceDTO> associations = new ArrayList<>();
    private VisualizationNodeType type;

    public List<VisualizationAttributeDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<VisualizationAttributeDTO> attributes) {
        this.attributes = attributes;
    }

    public List<VisualizationReferenceDTO> getAssociations() {
        return associations;
    }

    public void setAssociations(List<VisualizationReferenceDTO> associations) {
        this.associations = associations;
    }

    public VisualizationNodeType getType() {
        return type;
    }

    public void setType(VisualizationNodeType type) {
        this.type = type;
    }

    public void addAttribute(VisualizationAttributeDTO dto) {
        this.attributes.add(dto);
    }

    public void addAssociation(VisualizationReferenceDTO dto) {
        this.associations.add(dto);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
