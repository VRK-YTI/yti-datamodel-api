package com.csc.fi.ioapi.api.usermanagement;

public class UserDefinition {

    private final String displayName;
    private final String group;
    private final String mail;

    public UserDefinition(String displayName, String group, String mail) {
        this.displayName = displayName;
        this.group = group;
        this.mail = mail;
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
