package fi.vm.yti.datamodel.api.v2.dto.visualization;

import java.util.Objects;

public class VisualizationHiddenNodeDTO {
    private String identifier;
    private PositionDTO position;
    private String referenceTarget;
    private VisualizationReferenceType referenceType;
    private String origin;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

    public String getReferenceTarget() {
        return referenceTarget;
    }

    public void setReferenceTarget(String referenceTarget) {
        this.referenceTarget = referenceTarget;
    }

    public VisualizationReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(VisualizationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationHiddenNodeDTO that = (VisualizationHiddenNodeDTO) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
