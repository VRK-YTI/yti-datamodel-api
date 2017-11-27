/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import javafx.application.Application;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author malonen
 */
public class TermedAuthentication {

    public static HttpAuthenticationFeature getTermedAuth() {
        return HttpAuthenticationFeature.basic(ApplicationProperties.getDefaultTermAPIUser(), ApplicationProperties.getDefaultTermAPIUserSecret());
    }
    
}
