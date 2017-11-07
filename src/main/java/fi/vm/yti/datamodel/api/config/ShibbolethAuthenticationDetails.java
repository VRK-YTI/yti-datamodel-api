package fi.vm.yti.datamodel.api.config;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public final class ShibbolethAuthenticationDetails {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String group;

    public ShibbolethAuthenticationDetails(HttpServletRequest request) {
        this(
                getAttributeAsString(request, "mail"),
                getAttributeAsString(request, "givenname"),
                getAttributeAsString(request, "surname"),
                getAttributeAsString(request, "group")
        );
    }

    public ShibbolethAuthenticationDetails(String email, String firstName, String lastName, String group) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.group = group;
    }

    private static String getAttributeAsString(HttpServletRequest request, String attributeName) {

        Object attribute = requireNonNull(request.getAttribute(attributeName), "Request attribute missing: " + attributeName);
        return convertLatinToUTF8(attribute.toString());
    }

    private static String convertLatinToUTF8(String s) {
        return new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "ShibbolethAuthenticationDetails{" +
                "email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}