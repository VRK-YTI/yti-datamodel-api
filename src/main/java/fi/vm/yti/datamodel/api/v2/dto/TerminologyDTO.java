package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class TerminologyDTO {
    private Map<String, String> label = Map.of();
    private String uri;
    public TerminologyDTO(String uri) {
        this.uri = uri;
    }
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
}
