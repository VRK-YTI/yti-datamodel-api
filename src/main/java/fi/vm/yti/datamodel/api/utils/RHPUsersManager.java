package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;

import javax.ws.rs.client.ClientBuilder;
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
}
