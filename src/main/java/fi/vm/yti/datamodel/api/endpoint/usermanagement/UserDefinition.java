package fi.vm.yti.datamodel.api.endpoint.usermanagement;

import java.nio.charset.Charset;

public class UserDefinition {

    private final String displayName;
    private final String group;
    private final String mail;

    public UserDefinition(String displayName, String group, String mail) {
 
        /* FIXME: This should be done elsewhere ... filter of httpd config? */
        Charset iso88591charset = Charset.forName("ISO-8859-1");
        Charset utf8charset = Charset.forName("UTF-8");
        byte[] iso88591String = displayName.getBytes(iso88591charset);
        this.displayName = new String (iso88591String, utf8charset );
   
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
