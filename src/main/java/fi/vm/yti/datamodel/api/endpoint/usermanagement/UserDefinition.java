package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.config.ShibbolethAuthenticationDetails;

public class UserDefinition {

    private final String displayName;
    private final String group;
    private final String mail;

    public UserDefinition(String displayName, String group, String mail) {
        this.displayName = displayName;
        this.group = group;
        this.mail = mail;
    }

    public UserDefinition(ShibbolethAuthenticationDetails details) {
        this(details.getDisplayName(), details.getGroup(), details.getEmail());
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGroup() {
        return group;
    }

    public String getMail() {
        return mail;
    }
}
