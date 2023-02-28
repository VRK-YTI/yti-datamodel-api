package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexClass;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ClassQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.CountQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.atlas.lib.SetUtils;
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
            throw new RuntimeException(e);
        }
    }

    public SearchResponseDTO<IndexModel> searchModels(ModelSearchRequest request,
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

       public SearchResponseDTO<IndexClass> searchInternalClasses(ClassSearchRequest request) throws IOException {
        Set<String> namespaces = null;
        if(request.getFromAddedNamespaces() != null){
            namespaces = getNamespacesFromModel(request.getFromAddedNamespaces());
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

        SearchRequest build = ClassQueryFactory.createInternalClassQuery(request, namespaces, groupRestrictedNamespaces);
        SearchResponse<IndexClass> response = client.search(build, IndexClass.class);

        var result = new SearchResponseDTO<IndexClass>();
        result.setResponseObjects(response.hits().hits().stream()
                .map(Hit::source)
                .toList()
        );
        result.setTotalHitCount(response.hits().total().value());
        result.setPageFrom(request.getPageFrom());
        result.setPageSize(request.getPageSize());

        return result;
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
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Set<String> getNamespacesFromModel(String modelUri){
        var model = jenaService.getDataModel(modelUri);
        if(model != null){
            var resource = model.getResource(modelUri);
            var owlImport = MapperUtils.arrayPropertyToSet(resource, OWL.imports);
            var dcTermsRequires = MapperUtils.arrayPropertyToSet(resource, DCTerms.requires);
            return SetUtils.union(owlImport, dcTermsRequires);
        }
        return Collections.emptySet();
    }
}
