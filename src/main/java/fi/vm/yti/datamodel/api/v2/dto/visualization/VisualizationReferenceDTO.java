package fi.vm.yti.datamodel.api.v2.dto.visualization;

public class VisualizationReferenceDTO extends VisualizationItemDTO {
    private String referenceTarget;
    private VisualizationReferenceType referenceType;

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

}
