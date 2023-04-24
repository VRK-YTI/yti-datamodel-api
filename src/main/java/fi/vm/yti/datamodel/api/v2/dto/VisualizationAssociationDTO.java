package fi.vm.yti.datamodel.api.v2.dto;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VisualizationAssociationDTO {
    private String identifier;
    private Map<String, String> label;
    private List<String> path = new LinkedList<>();

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
