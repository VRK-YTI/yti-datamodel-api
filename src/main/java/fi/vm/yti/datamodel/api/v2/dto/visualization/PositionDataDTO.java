package fi.vm.yti.datamodel.api.v2.dto.visualization;

import java.util.Set;

public class PositionDataDTO {
    private String identifier;
    private Double x;
    private Double y;
    private Set<ReferenceTarget> referenceTargets = Set.of();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Set<ReferenceTarget> getReferenceTargets() {
        return referenceTargets;
    }

    public void setReferenceTargets(Set<ReferenceTarget> referenceTargets) {
        this.referenceTargets = referenceTargets;
    }

    public record ReferenceTarget(String target, String origin) {}
}
