package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisualizationClassDTO {
    private String identifier;
    private Map<String, String> label = Map.of();
    private Set<String> parentClasses = new HashSet<>();
    private PositionDTO position = new PositionDTO(0.0, 0.0);
    private List<VisualizationResourceDTO> attributes = List.of();
    private List<VisualizationResourceDTO> associations = List.of();

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

    public List<VisualizationResourceDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<VisualizationResourceDTO> attributes) {
        this.attributes = attributes;
    }

    public List<VisualizationResourceDTO> getAssociations() {
        return associations;
    }

    public void setAssociations(List<VisualizationResourceDTO> associations) {
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
