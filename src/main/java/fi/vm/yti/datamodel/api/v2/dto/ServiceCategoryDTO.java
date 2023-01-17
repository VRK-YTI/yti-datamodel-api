package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class ServiceCategoryDTO extends BaseDTO {

    private final String identifier;

    public ServiceCategoryDTO(String id, Map<String, String> label, String identifier) {
        super(id, label);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
