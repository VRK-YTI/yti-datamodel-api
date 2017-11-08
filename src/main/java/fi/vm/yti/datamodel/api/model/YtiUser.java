package fi.vm.yti.datamodel.api.model;

import java.util.*;

import static fi.vm.yti.datamodel.api.utils.CollectionUtil.containsAny;
import static fi.vm.yti.datamodel.api.utils.CollectionUtil.getOrInitializeSet;
import static fi.vm.yti.datamodel.api.utils.CollectionUtil.unmodifiable;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

public final class YtiUser {

    public static final YtiUser ANONYMOUS_USER =
            new YtiUser(true, "anonymous@example.org", "Anonymous", "User", false, false, emptyMap());

    private final boolean anonymous;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean superuser;
    private final boolean newlyCreated;
    private final Map<UUID, Set<Role>> rolesInOrganizations;
    private final Map<Role, Set<UUID>> organizationsInRole;

    public YtiUser(String email,
                   String firstName,
                   String lastName,
                   boolean superuser,
                   boolean newlyCreated,
                   Map<UUID, Set<Role>> rolesInOrganizations) {
        this(false, email, firstName, lastName, superuser, newlyCreated, rolesInOrganizations);
    }

    private YtiUser(boolean anonymous,
                    String email,
                    String firstName,
                    String lastName,
                    boolean superuser,
                    boolean newlyCreated,
                    Map<UUID, Set<Role>> rolesInOrganizations) {

        this.anonymous = anonymous;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.superuser = superuser;
        this.newlyCreated = newlyCreated;
        this.rolesInOrganizations = unmodifiable(rolesInOrganizations);

        HashMap<Role, Set<UUID>> organizationsInRole = new HashMap<>();

        for (Map.Entry<UUID, Set<Role>> entry : rolesInOrganizations.entrySet()) {
            for (Role role : entry.getValue()) {
                getOrInitializeSet(organizationsInRole, role).add(entry.getKey());
            }
        }

        this.organizationsInRole = unmodifiable(organizationsInRole);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public Map<UUID, Set<Role>> getRolesInOrganizations() {
        return rolesInOrganizations;
    }

    public Set<Role> getRoles(UUID organizationId) {
        return rolesInOrganizations.getOrDefault(organizationId, unmodifiableSet(emptySet()));
    }

    public Set<Role> getRoles(UUID... organizationsIds) {
        return getRoles(asList(organizationsIds));
    }

    public Set<Role> getRoles(Collection<UUID> organizationIds) {

        if (organizationIds.size() == 0) {
            return unmodifiableSet(emptySet());
        } else if (organizationIds.size() == 1) {
            return getRoles(organizationIds.iterator().next());
        } else {

            Set<Role> roles = EnumSet.noneOf(Role.class);

            for (UUID organizationId : organizationIds) {
                roles.addAll(getRoles(organizationId));
            }

            return unmodifiableSet(roles);
        }
    }

    public boolean isInRoleInAnyOrganization(Role role) {
        return !getOrganizations(role).isEmpty();
    }

    public boolean isInRole(Role role, UUID... organizationIds) {
        return isInRole(role, asList(organizationIds));
    }

    public boolean isInRole(Role role, Collection<UUID> organizationIds) {
        return isInAnyRole(singleton(role), organizationIds);
    }

    public boolean isInAnyRole(Collection<Role> roles, Collection<UUID> organizationIds) {
        return containsAny(getRoles(organizationIds), roles);
    }

    public Map<Role, Set<UUID>> getOrganizationsInRole() {
        return organizationsInRole;
    }

    public Set<UUID> getOrganizations(Role role) {
        return organizationsInRole.getOrDefault(role, unmodifiableSet(emptySet()));
    }

    public Set<UUID> getOrganizations(Role... roles) {
        return getOrganizations(asList(roles));
    }

    public Set<UUID> getOrganizations(Collection<Role> roles) {

        if (roles.size() == 0) {
            return unmodifiableSet(emptySet());
        } else if (roles.size() == 1) {
            return getOrganizations(roles.iterator().next());
        } else {

            Set<UUID> organizationIds = new HashSet<>();

            for (Role role : roles) {
                organizationIds.addAll(getOrganizations(role));
            }

            return unmodifiableSet(organizationIds);
        }
    }

    public boolean isInOrganization(UUID organizationId) {
        return isInOrganization(organizationId, Role.values());
    }

    public boolean isInOrganization(UUID organizationId, Role... roles) {
        return isInOrganization(organizationId, asList(roles));
    }

    public boolean isInOrganization(UUID organizationId, Collection<Role> roles) {
        return isInAnyOrganization(singleton(organizationId), roles);
    }

    public boolean isInAnyOrganization(Collection<UUID> organizationsIds, Collection<Role> roles) {
        return containsAny(getOrganizations(roles), organizationsIds);
    }

    @Override
    public String toString() {
        return "YtiUser{" +
                "email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", superuser=" + superuser +
                ", newlyCreated=" + newlyCreated +
                ", rolesInOrganizations=" + rolesInOrganizations +
                '}';
    }
}
