package fi.vm.yti.datamodel.api.v2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.IndexModelDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.CountQueryFactory;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchIndexService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchIndexService.class);
    private final CountQueryFactory countQueryFactory;
    private final RestHighLevelClient client;
    private final GroupManagementService groupManagementService;
    private final ObjectMapper objectMapper;

    public SearchIndexService(OpenSearchConnector openSearchConnector,
                              CountQueryFactory countQueryFactory,
                              GroupManagementService groupManagementService,
                              ObjectMapper objectMapper) {
        this.countQueryFactory = countQueryFactory;
        this.client = openSearchConnector.getClient();
        this.groupManagementService = groupManagementService;
        this.objectMapper = objectMapper;
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts() {
        SearchRequest query = countQueryFactory.createModelQuery();
        try {
            SearchResponse response = client.search(query, RequestOptions.DEFAULT);
            return countQueryFactory.parseResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelSearchResponse searchModels(ModelSearchRequest request,
                                            YtiUser user) {
        if (!user.isSuperuser()) {
            final Map<UUID, Set<Role>> rolesInOrganizations = user.getRolesInOrganizations();

            var orgIds = rolesInOrganizations.keySet()
                    .stream()
                    .map(UUID::toString)
                    .collect(Collectors.toSet());

            // show child organization's incomplete content for main organization users
            var childOrganizationIds = orgIds.stream()
                    .map(groupManagementService::getChildOrganizations)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            orgIds.addAll(childOrganizationIds);
            request.setIncludeIncompleteFrom(orgIds);
        }
        return searchModels(request);
    }

    private ModelSearchResponse searchModels(ModelSearchRequest request) {
        try {
            // TODO: implement search
            var response = client.search(new SearchRequest("models_v2"), RequestOptions.DEFAULT);
            var models = Arrays.stream(response.getHits().getHits())
                    .map(hit -> {
                        try {
                            return objectMapper.readValue(hit.getSourceAsString(), IndexModelDTO.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            var modelSearchResponse = new ModelSearchResponse();
            modelSearchResponse.setModels(models);
            modelSearchResponse.setTotalHitCount(response.getHits().getTotalHits().value);
            modelSearchResponse.setPageFrom(request.getPageFrom());
            modelSearchResponse.setPageSize(request.getPageSize());

            return modelSearchResponse;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
