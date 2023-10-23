package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class LinkDTO {

    private Map<String, String> name = Map.of();
    private String uri;
    private Map<String, String> description = Map.of();

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }
}
