/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.model.YtiUser;

import javax.servlet.http.HttpSession;
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

    @Override
    public boolean isInGroup(String group) {

        // TODO actual group to organization mapping
        UUID organizationID = UUID.randomUUID();

        return getUser().isInOrganization(organizationID, ADMIN, DATA_MODEL_EDITOR);
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

        // TODO model organizations mapping
        // ServiceDescriptionManager.isModelInGroup(model, this.getGroups());
        List<UUID> organizations = Collections.emptyList();

        return getUser().isInAnyOrganization(organizations, asList(ADMIN, DATA_MODEL_EDITOR));
    }
    
    @Override
    public boolean hasRightToEditGroup(String group) {

        return getUser().isSuperuser() || ApplicationProperties.getDebugMode() || isInGroup(group);
    }

    @Override
    public boolean isAdminOfGroup(String group) {

        if (getUser().isSuperuser()) {
            return true;
        }

        // TODO actual group to organization mapping
        UUID organizationID = UUID.randomUUID();

        return getUser().isInRole(ADMIN, organizationID);
    }

    private YtiUser getUser() {

        Object authenticatedUser = session.getAttribute("authenticatedUser");

        if (authenticatedUser != null) {
            return (YtiUser) authenticatedUser;
        } else {
            return YtiUser.ANONYMOUS_USER;
        }
    }
}
