/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.utils.PropertyUtil;

/**
 * @author malonen
 */
public final class ApplicationProperties {

    public final static String getSchemeId() {
        return PropertyUtil.getProperty("application.defaultScheme");
    }

    public final static String getEndpoint() {
        return PropertyUtil.getProperty("application.endpoint");
    }

    public final static boolean getDebugMode() {
        return Boolean.parseBoolean(PropertyUtil.getProperty("application.debug"));
    }

    public final static String getDebugUserFirstname() {
        return PropertyUtil.getProperty("application.debugUserFirstname");
    }

    public final static String getDebugUserLastname() {
        return PropertyUtil.getProperty("application.debugUserLastname");
    }

    public final static String getDebugUserEmail() {
        return PropertyUtil.getProperty("application.debugUserEmail");
    }

    public final static String getDebugUserSuper() {
        return PropertyUtil.getProperty("application.debugUserSuper");
    }

    public final static String getDebugAdress() {
        return PropertyUtil.getProperty("application.debugAdress");
    }

    public final static String getDefaultNamespace() {
        return PropertyUtil.getProperty("application.defaultNamespace");
    }

    public final static String getDefaultDomain() {
        return PropertyUtil.getProperty("application.defaultDomain");
    }

    public final static boolean getProvenanceMode() {
        return Boolean.parseBoolean(PropertyUtil.getProperty("application.provenance"));
    }

    public final static String getDefaultTermAPI() {
        return PropertyUtil.getProperty("application.defaultTermedAPI");
    }

    public final static String getDefaultTermAPIUser() {
        return PropertyUtil.getProperty("application.defaultTermedAPIUser");
    }

    public final static String getDefaultTermAPIUserSecret() {
        return PropertyUtil.getProperty("application.defaultTermedAPIUserSecret");
    }

    public final static String getDefaultGroupManagementAPI() {
        return PropertyUtil.getProperty("application.defaultGroupManagementAPI");
    }

    public final static String getPublicGroupManagementAPI() {
        return PropertyUtil.getProperty("application.publicGroupManagementAPI");
    }

    public final static String getPublicTermAPI() {
        return PropertyUtil.getProperty("application.publicTermedAPI");
    }


}
