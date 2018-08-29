
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
    private String publicTerminologyFrontend;
    private String publicSuomiCodeServerFrontend;
    private String publicGroupManagementFrontend;
    private String elasticHost;
    private String elasticPort;
    private String elasticCluster;
    private String defaultSuomiCodeServerAPI;
    private boolean devMode;

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

    public String getPublicGroupManagementFrontend() {
        return publicGroupManagementFrontend;
    }

    public void setPublicGroupManagementFrontend(String publicGroupManagementFrontend) {
        this.publicGroupManagementFrontend = publicGroupManagementFrontend;
    }

    public String getPublicSuomiCodeServerFrontend() {
        return publicSuomiCodeServerFrontend;
    }

    public void setPublicSuomiCodeServerFrontend(String publicSuomiCodeServerFrontend) {
        this.publicSuomiCodeServerFrontend = publicSuomiCodeServerFrontend;
    }

    public String getPublicTerminologyFrontend() {
        return publicTerminologyFrontend;
    }

    public void setPublicTerminologyFrontend(String publicTerminologyFrontend) {
        this.publicTerminologyFrontend = publicTerminologyFrontend;
    }


    public String getPublicTermedAPI() {
        return publicTermedAPI;
    }

    public void setPublicTermedAPI(String publicTermedAPI) {
        this.publicTermedAPI = publicTermedAPI;
    }

    public String getElasticHost() {
        return elasticHost;
    }

    public void setElasticHost(String elasticHost) {
        this.elasticHost = elasticHost;
    }

    public String getElasticPort() {
        return elasticPort;
    }

    public void setElasticPort(String elasticPort) {
        this.elasticPort = elasticPort;
    }

    public String getElasticCluster() {
        return elasticCluster;
    }

    public void setElasticCluster(String elasticCluster) {
        this.elasticCluster = elasticCluster;
    }

    public String getDefaultSuomiCodeServerAPI() { return this.defaultSuomiCodeServerAPI; }

    public void setDefaultSuomiCodeServerAPI(String defaultSuomiCodeServerAPI) { this.defaultSuomiCodeServerAPI = defaultSuomiCodeServerAPI; }

    public boolean getDevMode() { return this.devMode; }

    public void setDevMode(boolean devMode) { this.devMode = devMode; }

}
