package fi.vm.yti.datamodel.api.utils;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;

public final class GroupManagementUserRequest {

    private final UUID organizationId;
    private final List<String> role;

    // Jackson constructor
    private GroupManagementUserRequest() {
        this(randomUUID(), emptyList());
    }

    public GroupManagementUserRequest(UUID organizationId, List<String> role) {
        this.organizationId = organizationId;
        this.role = role;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public List<String> getRole() {
        return role;
    }
}
