package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class ServiceCategoryDTO {
    private final String id;
    private final Map<String, String> label;
    private final String identifier;

    public ServiceCategoryDTO(String id, Map<String, String> label, String identifier) {
        this.id = id;
        this.label = label;
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getLabel() {
        return label;
    }

}
