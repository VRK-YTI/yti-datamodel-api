package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public abstract class BaseDTO {
    private final String id;
    private final Map<String, String> label;

    protected BaseDTO(String id, Map<String, String> label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
