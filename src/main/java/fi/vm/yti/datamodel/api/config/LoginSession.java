/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.model.Role;
import fi.vm.yti.datamodel.api.model.YtiUser;
import fi.vm.yti.datamodel.api.utils.ServiceDescriptionManager;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static fi.vm.yti.datamodel.api.model.Role.ADMIN;
import static fi.vm.yti.datamodel.api.model.Role.DATA_MODEL_EDITOR;
import static java.util.Arrays.asList;

/**
 *
 * @author malonen
 */
public class LoginSession implements LoginInterface {

    private HttpSession session;
    private static final Logger logger = Logger.getLogger(LoginSession.class.getName());
    
    public LoginSession(HttpSession httpSession) {
        this.session = httpSession;
    }

    @Override
    public boolean isLoggedIn() {
        return !getUser().isAnonymous();
    }

    @Deprecated
    public boolean isInGroup(String group) {
        return false;
    }

    @Override
    public boolean isInOrganization(String org) {
        try {
            UUID organizationUUID = UUID.fromString(org);
            return getUser().isInOrganization(organizationUUID);
        } catch(IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public String getDisplayName() {
        return getUser().getDisplayName();
    }
    
    public boolean isSuperAdmin() {
        return getUser().isSuperuser();
    }

    @Override
    public String getEmail() {
       return getUser().getEmail();
    }

    @Override
    public boolean hasRightToEditModel(String model) {

        if (getUser().isSuperuser()) {
            return true;
        }

        Collection<UUID> modelOrganizations = ServiceDescriptionManager.getModelOrganizations(model);

        return getUser().isInAnyOrganization(modelOrganizations, asList(ADMIN, DATA_MODEL_EDITOR));
    }

    @Deprecated
    public boolean isAdminOfGroup(String group) {
        return false;
    }

    @Deprecated
    public boolean hasRightToEditGroup(String group) {
        return false;
    }

    public boolean isUserInOrganization(UUID organization) {
        return getUser().isInOrganization(organization, asList(ADMIN, DATA_MODEL_EDITOR));
    }

    public boolean isUserInOrganization(List<UUID> organizations) {
        return getUser().isInAnyOrganization(organizations, asList(ADMIN, DATA_MODEL_EDITOR));
    }

    public YtiUser getUser() {

        Object authenticatedUser = session.getAttribute("authenticatedUser");

        if (authenticatedUser != null) {
            return (YtiUser) authenticatedUser;
        } else {
            return YtiUser.ANONYMOUS_USER;
        }
    }
}
