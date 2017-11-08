/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.utils.PropertyUtil;

/**
 *
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

public final static String getGroupDomain() {
    return PropertyUtil.getProperty("application.groupdomain");
}

public final static String getDebugGroups() {
    return PropertyUtil.getProperty("application.debugGroup");  
}

public final static String getDebugAdress() {
    return PropertyUtil.getProperty("application.debugAdress");  
}

public final static String getDefaultNamespace() {
     return getDefaultDomain()+"ns/";
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

public static String getDefaultGroupManagementAPI() {
    return PropertyUtil.getProperty("application.defaultGroupManagementAPI");
}

}
