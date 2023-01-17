package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public abstract class BaseDTO {
    private final String id;
    private final Map<String, String> label;

    public BaseDTO(String id, Map<String, String> label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getLabel() {
        return label;
    }
}
