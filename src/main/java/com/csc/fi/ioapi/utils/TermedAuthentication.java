/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author malonen
 */
public class TermedAuthentication {
    
    /* TODO: Add to pom ? */
    
    public static HttpAuthenticationFeature getTermedAuth() {
        return HttpAuthenticationFeature.basic("admin", "admin");
    }
    
}
