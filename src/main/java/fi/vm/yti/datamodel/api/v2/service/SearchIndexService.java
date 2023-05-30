package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.endpoint.error.OpenSearchException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.SearchResponseDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.CountQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.QueryFactoryUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ResourceQueryFactory;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchIndexService {

    private final OpenSearchClient client;
    private final GroupManagementService groupManagementService;
    private final JenaService jenaService;

    public SearchIndexService(OpenSearchConnector openSearchConnector,
                              GroupManagementService groupManagementService, JenaService jenaService) {
        this.client = openSearchConnector.getClient();
        this.groupManagementService = groupManagementService;
        this.jenaService = jenaService;
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts() {
        var query = CountQueryFactory.createModelQuery();
        try {
            var response = client.search(query, IndexModel.class);
            return CountQueryFactory.parseResponse(response);
        } catch (IOException e) {
            throw new OpenSearchException(e.getMessage(), OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        }
    }

    public SearchResponseDTO<IndexModel> searchModels(ModelSearchRequest request, YtiUser user) {
        try {
            if (!user.isSuperuser()) {
                request.setIncludeIncompleteFrom(getOrganizationsForUser(user));
            }

            var build = ModelQueryFactory.createModelQuery(request);
            var response = client.search(build, IndexModel.class);

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
            throw new OpenSearchException(e.getMessage(), OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        }
    }

    public SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, YtiUser user) throws IOException {
        Set<String> allowedDatamodels = new HashSet<>();
        if(!user.isSuperuser()){
                var organizations = getOrganizationsForUser(user);
                var modelRequest = new ModelSearchRequest();
                modelRequest.setPageSize(QueryFactoryUtils.INTERNAL_SEARCH_PAGE_SIZE);
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

    /**
     * Get model specific restrictions for a resource search
     * @param modelType Model type
     * @param groups Service category groups
     * @return List of DataModel URIs
     */
    private List<String> getModelSpecificRestrictions(ModelType modelType, Set<String> groups){
        if(modelType == null && (groups == null || groups.isEmpty())){
            //Skip the search all together if no extra filtering needs to be done
            return Collections.emptyList();
        }
        var modelRequest = new ModelSearchRequest();
        modelRequest.setPageSize(QueryFactoryUtils.INTERNAL_SEARCH_PAGE_SIZE);
        if(modelType != null){
            modelRequest.setType(Set.of(modelType));
        }
        if(groups != null && !groups.isEmpty()){
            modelRequest.setGroups(groups);
        }
        var build = ModelQueryFactory.createModelQuery(modelRequest);
        try {
            var response = client.search(build, IndexModel.class);
            return response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(hit -> hit.source().getId()).toList();
        } catch (IOException e) {
            throw new OpenSearchException(e.getMessage(), OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        }
    }

   public SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, Set<String> allowedDatamodels) throws IOException {
        var namespaces = new ArrayList<String>();
        if(request.getLimitToDataModel() != null && !request.getLimitToDataModel().isBlank()){
            namespaces.add(request.getLimitToDataModel());
        }

        if(request.isFromAddedNamespaces()){
            if(request.getLimitToDataModel() == null){
                throw new OpenSearchException("limitToDataModel cannot be empty if getting from added namespace", OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE);
            }
            getNamespacesFromModel(request.getLimitToDataModel(), namespaces);
        }

        var restrictedDataModels = getModelSpecificRestrictions(request.getLimitToModelType(), request.getGroups());
        var build = ResourceQueryFactory.createInternalResourceQuery(request, namespaces, restrictedDataModels, allowedDatamodels);
        var response = client.search(build, IndexResource.class);

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

    public SearchResponseDTO<IndexResource> findResourcesByURI(Set<String> resourceURIs) throws IOException {
        var response = client.search(ResourceQueryFactory.createFindResourcesByURIQuery(resourceURIs), IndexResource.class);
        var result = new SearchResponseDTO<IndexResource>();
        result.setResponseObjects(response.hits().hits().stream()
                .map(Hit::source)
                .toList()
        );
        result.setTotalHitCount(response.hits().total().value());
        return result;
    }

    private void getNamespacesFromModel(String modelUri, List<String> namespaces){
        var model = jenaService.getDataModel(modelUri);
        var resource = model.getResource(modelUri);
        namespaces.addAll(MapperUtils.arrayPropertyToList(resource, OWL.imports));
        namespaces.addAll(MapperUtils.arrayPropertyToList(resource, DCTerms.requires));
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
