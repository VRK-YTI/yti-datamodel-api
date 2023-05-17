package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;
import java.util.UUID;

public class OrganizationDTO {
    private final String id;
    private final Map<String, String> label;
    private final UUID parentOrganization;

    public OrganizationDTO(String id, Map<String, String> label, UUID parentOrganization) {
        this.id = id;
        this.label = label;
        this.parentOrganization = parentOrganization;
    }

    public UUID getParentOrganization() {
        return parentOrganization;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getLabel() {
        return label;
    }
}
