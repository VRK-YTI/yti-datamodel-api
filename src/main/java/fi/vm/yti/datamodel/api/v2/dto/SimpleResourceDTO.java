package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class SimpleResourceDTO {

    private String uri;
    private Map<String, String> label;
    private String identifier;
    private String modelId;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String namespace) {
        this.modelId = namespace;
    }
}
