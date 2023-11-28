package fi.vm.yti.datamodel.api.v2.dto;

public class ResourceCommonDTO {
    private String created;
    private String modified;
    private UserDTO modifier;
    private UserDTO creator;
    private String uri;

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public UserDTO getModifier() {
        return modifier;
    }

    public void setModifier(UserDTO modifier) {
        this.modifier = modifier;
    }

    public UserDTO getCreator() {
        return creator;
    }

    public void setCreator(UserDTO creator) {
        this.creator = creator;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
