/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import com.csc.fi.ioapi.utils.PropertyUtil;

/**
 *
 * @author malonen
 */
public class ApplicationProperties {    
    
public static String getEndpoint() {
    return PropertyUtil.getProperty("application.endpoint");
}

public static boolean getDebugMode() {
    return Boolean.parseBoolean(PropertyUtil.getProperty("application.debug"));
} 

public static String getGroupDomain() {
    return PropertyUtil.getProperty("application.groupdomain");
} 

public static String getDebugAdress() {
    return PropertyUtil.getProperty("application.debugAdress");  
}

public static String getDefaultNamespace() {
    return PropertyUtil.getProperty("application.defaultNamespace");  
}
    
}
