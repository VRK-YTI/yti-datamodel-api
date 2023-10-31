package fi.vm.yti.datamodel.api.v2.dto.visualization;

import java.util.HashSet;
import java.util.Set;

public class VisualizationNodeDTO extends VisualizationItemDTO {
    private PositionDTO position = new PositionDTO(0.0, 0.0);
    private VisualizationNodeType type;
    private Set<VisualizationReferenceDTO> references = new HashSet<>();

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

    public VisualizationNodeType getType() {
        return type;
    }

    public void setType(VisualizationNodeType type) {
        this.type = type;
    }

    public Set<VisualizationReferenceDTO> getReferences() {
        return references;
    }

    public void setReferences(Set<VisualizationReferenceDTO> references) {
        this.references = references;
    }

    public void addReference(VisualizationReferenceDTO dto) {
        this.references.add(dto);
    }
}
