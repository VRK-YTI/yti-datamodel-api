package fi.vm.yti.datamodel.api.v2.dto;

public class VisualizationReferenceDTO extends VisualizationItemDTO {
    private String referenceTarget;

    public String getReferenceTarget() {
        return referenceTarget;
    }

    public void setReferenceTarget(String referenceTarget) {
        this.referenceTarget = referenceTarget;
    }
}
