package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;
import java.util.UUID;

public class OrganizationDTO extends BaseDTO {

    private final UUID parentOrganization;

    public OrganizationDTO(String id, Map<String, String> label, UUID parentOrganization) {
        super(id, label);
        this.parentOrganization = parentOrganization;
    }

    public UUID getParentOrganization() {
        return parentOrganization;
    }
}
