package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;

public class VisualizationClassDTO {
    private String identifier;
    private Map<String, String> label = Map.of();
    private Set<String> parentClasses = new HashSet<>();
    private PositionDTO position = new PositionDTO(0.0, 0.0);
    private List<VisualizationAttributeDTO> attributes = new ArrayList<>();
    private List<VisualizationAssociationDTO> associations = new ArrayList<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

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

    public List<VisualizationAssociationDTO> getAssociations() {
        return associations;
    }

    public void setAssociations(List<VisualizationAssociationDTO> associations) {
        this.associations = associations;
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

}
