package fi.vm.yti.datamodel.api.v2.dto;

public class UserDTO {
    private String id;
    private String name;

    public UserDTO(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
