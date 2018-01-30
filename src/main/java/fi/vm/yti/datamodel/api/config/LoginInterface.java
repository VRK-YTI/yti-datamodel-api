/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author malonen
 */
public interface LoginInterface {
    
    public boolean isLoggedIn();
    
    public boolean isSuperAdmin();

    @Deprecated
    public boolean isInGroup(String group);

    public boolean isInOrganization(String org);

    @Deprecated
    public boolean isAdminOfGroup(String group);
    
    public boolean hasRightToEditModel(List<UUID> organization);

    @Deprecated
    public boolean hasRightToEditGroup(String resource);

    public boolean isUserInOrganization(UUID organization);

    public boolean isUserInOrganization(List<UUID> organization);

    public String getDisplayName();
    
    public String getEmail();

}
