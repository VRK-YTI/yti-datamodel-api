package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import fi.vm.yti.datamodel.api.model.YtiUser;

public class UserDefinition {

    private final String mail;
    private final String displayName;
    @Deprecated
    private final String group;
    private final YtiUser user;

    public UserDefinition(YtiUser user) {
        this(user.getDisplayName(), "", user.getEmail());
    }

    public UserDefinition(String displayName, String group, String mail) {
        this(displayName, group, mail, null);
    }

    public UserDefinition(String displayName, String group, String mail, YtiUser user) {
        this.displayName = displayName;
        this.group = group;
        this.mail = mail;
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Deprecated
    public String getGroup() {
        return group;
    }

    public String getMail() {
        return mail;
    }
}
