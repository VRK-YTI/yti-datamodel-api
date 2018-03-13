package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Service
public class RHPUsersManager {

    private final ApplicationProperties properties;
    private final ClientFactory clientFactory;
    private final boolean fakeLoginAllowed;

    RHPUsersManager(ApplicationProperties properties,
                    ClientFactory clientFactory,
                    @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {

        this.properties = properties;
        this.clientFactory = clientFactory;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    public List<GroupManagementUser> getFakeableUsers() {

        if (fakeLoginAllowed) {

            String url = properties.getDefaultGroupManagementAPI() + "users";

            return clientFactory.create()
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<List<GroupManagementUser>>() {});
        } else {
            return Collections.emptyList();
        }
    }

    public List<GroupManagementUserRequest> getUserRequests(String email) {

        String url = properties.getDefaultGroupManagementAPI() + "requests";

        return clientFactory.create()
                .target(url)
                .queryParam("email", email)
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<GroupManagementUserRequest>>() {});
    }

    public void sendUserRequests(String email, String organizationId) {

        String url = properties.getDefaultGroupManagementAPI() + "request";

        clientFactory.create()
                .target(url)
                .queryParam("email", email)
                .queryParam("role", Role.DATA_MODEL_EDITOR.toString())
                .queryParam("organizationId", organizationId)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null));
    }
}
