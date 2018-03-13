
package fi.vm.yti.datamodel.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public final class ApplicationProperties {

    private String defaultScheme;
    private String endpoint;
    private String defaultNamespace;
    private boolean provenance;
    private String defaultTermedAPI;
    private String defaultTermedAPIUser;
    private String defaultTermedAPIUserSecret;
    private String defaultGroupManagementAPI;
    private String publicGroupManagementAPI;
    private String publicTermedAPI;

    public String getDefaultScheme() {
        return defaultScheme;
    }

    public void setDefaultScheme(String defaultScheme) {
        this.defaultScheme = defaultScheme;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public boolean isProvenance() {
        return provenance;
    }

    public void setProvenance(boolean provenance) {
        this.provenance = provenance;
    }

    public String getDefaultTermedAPI() {
        return defaultTermedAPI;
    }

    public void setDefaultTermedAPI(String defaultTermedAPI) {
        this.defaultTermedAPI = defaultTermedAPI;
    }

    public String getDefaultTermedAPIUser() {
        return defaultTermedAPIUser;
    }

    public void setDefaultTermedAPIUser(String defaultTermedAPIUser) {
        this.defaultTermedAPIUser = defaultTermedAPIUser;
    }

    public String getDefaultTermedAPIUserSecret() {
        return defaultTermedAPIUserSecret;
    }

    public void setDefaultTermedAPIUserSecret(String defaultTermedAPIUserSecret) {
        this.defaultTermedAPIUserSecret = defaultTermedAPIUserSecret;
    }

    public String getDefaultGroupManagementAPI() {
        return defaultGroupManagementAPI;
    }

    public void setDefaultGroupManagementAPI(String defaultGroupManagementAPI) {
        this.defaultGroupManagementAPI = defaultGroupManagementAPI;
    }

    public String getPublicGroupManagementAPI() {
        return publicGroupManagementAPI;
    }

    public void setPublicGroupManagementAPI(String publicGroupManagementAPI) {
        this.publicGroupManagementAPI = publicGroupManagementAPI;
    }

    public String getPublicTermedAPI() {
        return publicTermedAPI;
    }

    public void setPublicTermedAPI(String publicTermedAPI) {
        this.publicTermedAPI = publicTermedAPI;
    }
}
