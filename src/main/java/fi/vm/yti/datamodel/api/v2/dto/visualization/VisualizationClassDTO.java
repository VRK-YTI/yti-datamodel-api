package fi.vm.yti.datamodel.api.v2.dto.visualization;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisualizationClassDTO extends VisualizationItemDTO {

    private Set<String> parentClasses = new HashSet<>();
    private PositionDTO position = new PositionDTO(0.0, 0.0);
    private List<VisualizationAttributeDTO> attributes = new ArrayList<>();
    private List<VisualizationReferenceDTO> associations = new ArrayList<>();
    private List<VisualizationReferenceDTO> attributeReferences = new ArrayList<>();
    private List<VisualizationReferenceDTO> associationReferences = new ArrayList<>();
    private VisualizationNodeType type;

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

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

    public List<VisualizationReferenceDTO> getAttributeReferences() {
        return attributeReferences;
    }

    public void setAttributeReferences(List<VisualizationReferenceDTO> attributeReferences) {
        this.attributeReferences = attributeReferences;
    }

    public List<VisualizationReferenceDTO> getAssociationReferences() {
        return associationReferences;
    }

    public void setAssociationReferences(List<VisualizationReferenceDTO> associationReferences) {
        this.associationReferences = associationReferences;
    }

    public VisualizationNodeType getType() {
        return type;
    }

    public void setType(VisualizationNodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Set<String> getParentClasses() {
        return parentClasses;
    }

    public void setParentClasses(Set<String> parentClasses) {
        this.parentClasses = parentClasses;
    }

    public void addAttribute(VisualizationAttributeDTO dto) {
        this.attributes.add(dto);
    }

    public void addAssociation(VisualizationReferenceDTO dto) {
        this.associations.add(dto);
    }

    public void addAttributeReference(VisualizationReferenceDTO dto) {
        this.attributeReferences.add(dto);
    }

    public void addAssociationReference(VisualizationReferenceDTO dto) {
        this.associationReferences.add(dto);
    }
}
