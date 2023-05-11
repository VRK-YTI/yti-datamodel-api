package fi.vm.yti.datamodel.api.v2.dto;

import java.util.List;
import java.util.Map;

public class ExternalClassDTO {
    private Map<String, String> label;
    private String uri;
    private List<ExternalResourceDTO> attributes;
    private List<ExternalResourceDTO> associations;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<ExternalResourceDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ExternalResourceDTO> attributes) {
        this.attributes = attributes;
    }

    public List<ExternalResourceDTO> getAssociations() {
        return associations;
    }

    public void setAssociations(List<ExternalResourceDTO> associations) {
        this.associations = associations;
    }
}
