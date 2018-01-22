package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.Role;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

import static fi.vm.yti.datamodel.api.utils.SSLContextFactory.naiveSSLContext;

public class RHPUsersManager {

    public static List<GroupManagementUser> getFakeableUsers() {

        if (ApplicationProperties.getDebugMode()) {

            String url = ApplicationProperties.getDefaultGroupManagementAPI() + "users";

            return ClientBuilder.newBuilder()
                    .sslContext(naiveSSLContext()).build()
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<List<GroupManagementUser>>() {});
        } else {
            return Collections.emptyList();
        }
    }

    public static List<GroupManagementUserRequest> getUserRequests(String email) {

        String url = ApplicationProperties.getDefaultGroupManagementAPI() + "requests";

        List<GroupManagementUserRequest> result = ClientBuilder.newBuilder()
                .sslContext(naiveSSLContext()).build()
                .target(url)
                .queryParam("email", email)
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<GroupManagementUserRequest>>() {
                });

        return result;
    }

    public static void sendUserRequests(String email, String organizationId) {

        String url = ApplicationProperties.getDefaultGroupManagementAPI() + "request";

        ClientBuilder.newBuilder()
                .sslContext(naiveSSLContext()).build()
                .target(url)
                .queryParam("email", email)
                .queryParam("role", Role.DATA_MODEL_EDITOR.toString())
                .queryParam("organizationId", organizationId)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null));
    }
}
