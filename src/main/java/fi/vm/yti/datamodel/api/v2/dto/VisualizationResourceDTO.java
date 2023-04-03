package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class VisualizationResourceDTO {
    private String identifier;
    private Map<String, String> label;

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
}
