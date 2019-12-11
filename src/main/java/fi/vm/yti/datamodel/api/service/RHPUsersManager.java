package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.GroupManagementUserDTO;
import fi.vm.yti.datamodel.api.model.GroupManagementUserRequestDTO;
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

    public List<GroupManagementUserDTO> getFakeableUsers() {

        if (fakeLoginAllowed) {

            String url = properties.getDefaultGroupManagementAPI() + "users";

            return clientFactory.create()
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<GroupManagementUserDTO>>() {
                });
        } else {
            return Collections.emptyList();
        }
    }

    public List<GroupManagementUserRequestDTO> getUserRequests(String userId) {

        String url = properties.getPrivateGroupManagementAPI() + "requests";

        return clientFactory.create()
            .target(url)
            .queryParam("userId", userId)
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<GroupManagementUserRequestDTO>>() {
            });
    }

    public void sendUserRequests(String userId,
                                 String organizationId) {

        String url = properties.getPrivateGroupManagementAPI() + "request";

        clientFactory.create()
            .target(url)
            .queryParam("userId", userId)
            .queryParam("role", Role.DATA_MODEL_EDITOR.toString())
            .queryParam("organizationId", organizationId)
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(null));
    }
}
