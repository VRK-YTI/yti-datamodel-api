package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.IndexModelDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.index.DataModelDocument;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.CountQueryFactory;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
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
    private final OpenSearchClient client;
    private final GroupManagementService groupManagementService;

    public SearchIndexService(OpenSearchConnector openSearchConnector,
                              CountQueryFactory countQueryFactory,
                              GroupManagementService groupManagementService) {
        this.countQueryFactory = countQueryFactory;
        this.client = openSearchConnector.getClient();
        this.groupManagementService = groupManagementService;
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts() {
        SearchRequest query = countQueryFactory.createModelQuery();
        try {
            SearchResponse<DataModelDocument> response = client.search(query, DataModelDocument.class);
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
            SearchRequest build = new SearchRequest.Builder().index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL).build();
            SearchResponse<IndexModelDTO> response = client.search(build, IndexModelDTO.class);

            var modelSearchResponse = new ModelSearchResponse();
            modelSearchResponse.setModels(response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList())
            );
            modelSearchResponse.setTotalHitCount(response.hits().total().value());
            modelSearchResponse.setPageFrom(request.getPageFrom());
            modelSearchResponse.setPageSize(request.getPageSize());

            return modelSearchResponse;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
