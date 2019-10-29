package fi.vm.yti.datamodel.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class GroupManagementUserDTO {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final LocalDateTime removedDateTime;

    // Jackson constructor
    private GroupManagementUserDTO() {
        this(UUID.randomUUID(), "", "", "", null);
    }

    public GroupManagementUserDTO(UUID id,
                                  String email,
                                  String firstName,
                                  String lastName,
                                  LocalDateTime removedDateTime) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.removedDateTime = removedDateTime;
    }

    public UUID getId() {
        return id;
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

    public LocalDateTime getRemovedDateTime() {
        return removedDateTime;
    }
}
