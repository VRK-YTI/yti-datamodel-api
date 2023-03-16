package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.GroupManagementOrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.GroupManagementUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static fi.vm.yti.datamodel.api.v2.mapper.OrganizationMapper.mapGroupManagementOrganizationToModel;

@Service
public class GroupManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupManagementService.class);
    private final JenaService jenaService;

    private final WebClient webClient;

    @Value("${fake.login.allowed:false}")
    private boolean fakeLoginAllowed;

    public GroupManagementService(
            @Qualifier("groupManagementClient") WebClient webClient,
            JenaService jenaService) {
        this.jenaService = jenaService;
        this.webClient = webClient;
    }

    public void initOrganizations() {

        try {
            var organizations = webClient.get().uri(builder -> builder
                            .path("/organizations")
                            .queryParam("onlyValid", "true")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GroupManagementOrganizationDTO>>() {
                    })
                    .block();

            var model = mapGroupManagementOrganizationToModel(organizations);
            jenaService.saveOrganizations(model);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void updateUsers() {
        // TODO:
    }

    public List<UUID> getChildOrganizations(UUID orgId) {
        return List.of();
    }

    public List<GroupManagementUserDTO> getFakeableUsers() {

        if (fakeLoginAllowed) {

            try {
                return webClient.get().uri(builder -> builder
                                .path("/users")
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<GroupManagementUserDTO>>() {
                        }).block();

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return List.of();
    }
}
