package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.endpoint.error.OpenSearchException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ResourceQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.CountQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
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
    private final JenaService jenaService;

    public SearchIndexService(OpenSearchConnector openSearchConnector,
                              CountQueryFactory countQueryFactory,
                              GroupManagementService groupManagementService, JenaService jenaService) {
        this.countQueryFactory = countQueryFactory;
        this.client = openSearchConnector.getClient();
        this.groupManagementService = groupManagementService;
        this.jenaService = jenaService;
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts() {
        SearchRequest query = countQueryFactory.createModelQuery();
        try {
            SearchResponse<IndexModel> response = client.search(query, IndexModel.class);
            return countQueryFactory.parseResponse(response);
        } catch (IOException e) {
            throw new OpenSearchException(e.getMessage(), OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        }
    }

    public SearchResponseDTO<IndexModel> searchModels(ModelSearchRequest request,
                                                      YtiUser user) {
        if (!user.isSuperuser()) {
            request.setIncludeIncompleteFrom(getOrganizationsForUser(user));
        }
        return searchModels(request);
    }

    private SearchResponseDTO<IndexModel> searchModels(ModelSearchRequest request) {
        try {
            SearchRequest build = ModelQueryFactory.createModelQuery(request);
            SearchResponse<IndexModel> response = client.search(build, IndexModel.class);

            var modelSearchResponse = new SearchResponseDTO<IndexModel>();
            modelSearchResponse.setResponseObjects(response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList())
            );
            modelSearchResponse.setTotalHitCount(response.hits().total().value());
            modelSearchResponse.setPageFrom(request.getPageFrom());
            modelSearchResponse.setPageSize(request.getPageSize());

            return modelSearchResponse;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new OpenSearchException(e.getMessage(), OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        }
    }

    public SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, YtiUser user) throws IOException {
        Set<String> allowedDatamodels = new HashSet<>();
        if(!user.isSuperuser()){
                var organizations = getOrganizationsForUser(user);
                var modelRequest = new ModelSearchRequest();
                modelRequest.setOrganizations(organizations);
                modelRequest.setIncludeIncompleteFrom(organizations);
                var build = ModelQueryFactory.createModelQuery(modelRequest);
                var response = client.search(build, IndexModel.class);
                allowedDatamodels = response.hits().hits().stream()
                        .filter(hit -> hit.source() != null)
                        .map(hit -> hit.source().getId()).collect(Collectors.toSet());
        }
        return searchInternalResources(request, allowedDatamodels);
    }

       public SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, Set<String> allowedDatamodels) throws IOException {
        Set<String> namespaces = new HashSet<>();
        if(request.getLimitToDataModel() != null && !request.getLimitToDataModel().isBlank()){
            namespaces.add(request.getLimitToDataModel());
        }

        if(request.isFromAddedNamespaces()){
            if(request.getLimitToDataModel() == null){
                throw new OpenSearchException("limitToDataModel cannot be empty if getting from added namespace", OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE);
            }
            getNamespacesFromModel(request.getLimitToDataModel(), namespaces);
        }

        Set<String> groupRestrictedNamespaces = null;
        if(request.getGroups() != null){
            var modelSearchRequest = new ModelSearchRequest();
            modelSearchRequest.setGroups(request.getGroups());
            SearchRequest fromModel = ModelQueryFactory.createModelQuery(modelSearchRequest);
            SearchResponse<IndexModel> modelResponse = client.search(fromModel, IndexModel.class);
            groupRestrictedNamespaces = modelResponse.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(hit -> hit.source().getId()).collect(Collectors.toSet());
        }

        SearchRequest build = ResourceQueryFactory.createInternalResourceQuery(request, namespaces, groupRestrictedNamespaces, allowedDatamodels);
        SearchResponse<IndexResource> response = client.search(build, IndexResource.class);

        var result = new SearchResponseDTO<IndexResource>();
        result.setResponseObjects(response.hits().hits().stream()
                .map(Hit::source)
                .toList()
        );
        result.setTotalHitCount(response.hits().total().value());
        result.setPageFrom(request.getPageFrom());
        result.setPageSize(request.getPageSize());

        return result;
    }

    private void getNamespacesFromModel(String modelUri, Set<String> namespaces){
        var model = jenaService.getDataModel(modelUri);
        if(model != null){
            var resource = model.getResource(modelUri);
            namespaces.addAll(MapperUtils.arrayPropertyToSet(resource, OWL.imports));
            namespaces.addAll(MapperUtils.arrayPropertyToSet(resource, DCTerms.requires));
        }
    }

    private Set<UUID> getOrganizationsForUser(YtiUser user){
        final Map<UUID, Set<Role>> rolesInOrganizations = user.getRolesInOrganizations();

        var orgIds = new HashSet<>(rolesInOrganizations.keySet());

        // show child organization's incomplete content for main organization users
        var childOrganizationIds = orgIds.stream()
                .map(groupManagementService::getChildOrganizations)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        orgIds.addAll(childOrganizationIds);
        return orgIds;
    }
}
