package fi.vm.yti.datamodel.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.GroupManagementUserDTO;

@Service
public class GroupManagementService {

    private static final Logger logger = LoggerFactory.getLogger(GroupManagementService.class.getName());

    private Map<UUID,GroupManagementUserDTO> users;
    private final ClientFactory clientFactory;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroupManagementService(ClientFactory clientFactory,
                                  ApplicationProperties applicationProperties,
                                  ObjectMapper objectMapper) {
        this.clientFactory = clientFactory;
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
        this.users = new HashMap<>();
        logger.info("Initializing Group management?");
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void fetchUsers() {
        updateUsers();
    }

    public void updateUsers() {
        final String url = applicationProperties.getDefaultGroupManagementAPI().replace("public-api","private-api")+"users";
        logger.debug(url);
        Client client = clientFactory.create();
        List<GroupManagementUserDTO> userList = client.target(url).request(MediaType.APPLICATION_JSON).get(new GenericType<List<GroupManagementUserDTO>>(){});
        userList.forEach(user -> users.put(user.getId(), user));
        logger.debug("Updated "+userList.size()+" users");
    }

}
