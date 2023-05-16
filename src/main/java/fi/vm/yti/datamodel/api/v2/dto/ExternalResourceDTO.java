package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class ExternalResourceDTO {
    private Map<String, String> label;
    private String uri;

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
