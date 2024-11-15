package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelSearchResultDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.endpoint.error.OpenSearchException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.*;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.QueryFactoryUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ResourceQueryFactory;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchIndexService {

    private final OpenSearchClientWrapper client;
    private final GroupManagementService groupManagementService;
    private final TerminologyService terminologyService;
    private final ResourceService resourceService;
    private final CoreRepository coreRepository;

    public SearchIndexService(OpenSearchClientWrapper client,
                              GroupManagementService groupManagementService,
                              TerminologyService terminologyService,
                              ResourceService resourceService,
                              CoreRepository coreRepository) {
        this.client = client;
        this.groupManagementService = groupManagementService;
        this.terminologyService = terminologyService;
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
    }

    /**
     * Search resources with the same keyword that was used to search a model
     * @return response containing resources
     */
    private SearchResponseDTO<IndexResource> getMatchingResources(ModelSearchRequest request) {
        var resourceSearchRequest = new ResourceSearchRequest();
        // use the same query string for resources that was used for models
        resourceSearchRequest.setQuery(request.getQuery());

        var resourceQuery = ResourceQueryFactory.createResourceQuery(
                resourceSearchRequest,
                Collections.emptyList());
        return client.search(resourceQuery, IndexResource.class);
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts(ModelSearchRequest searchRequest, YtiUser user) {
        if (!user.isSuperuser()) {
            searchRequest.setIncludeDraftFrom(groupManagementService.getOrganizationsForUser(user));
        }

        // if we were provided a search string, let's search for resources with the same string
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().isBlank()) {
            SearchResponseDTO<IndexResource> resources = this.getMatchingResources(searchRequest);

            // now let's add those matching models to the model search request
            searchRequest.setAdditionalModelIds(resources
                    .getResponseObjects()
                    .stream()
                    .map(r -> (r.getVersionIri() != null ? r.getVersionIri() : r.getIsDefinedBy()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        var query = ModelQueryFactory.createModelCountQuery(searchRequest, user.isSuperuser());
        return ModelQueryFactory.parseModelCountResponse(client.searchResponse(query, IndexModel.class));
    }

    public SearchResponseDTO<ModelSearchResultDTO> searchModels(ModelSearchRequest request, YtiUser user) {

        if (!user.isSuperuser()) {
            request.setIncludeDraftFrom(groupManagementService.getOrganizationsForUser(user));
        }

        // if we were provided a search string, let's search for resources with the same string
        SearchResponseDTO<IndexResource> resources = null;
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            resources = this.getMatchingResources(request);

            // now let's add those matching models to the model search request
            request.setAdditionalModelIds(resources
                    .getResponseObjects()
                    .stream()
                    .map(r -> (r.getVersionIri() != null ? r.getVersionIri() : r.getIsDefinedBy()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        var query = ModelQueryFactory.createModelQuery(request, user.isSuperuser());
        var models = client.search(query, ModelSearchResultDTO.class);

        if (resources != null) {
            for (var searchResult : models.getResponseObjects()) {
                // link each matching resource to the model, if the versioned id matches
                resources.getResponseObjects().stream()
                        .filter(o -> (o.getVersionIri() != null ?
                                o.getVersionIri() :
                                o.getIsDefinedBy()).equals(searchResult.getId()))
                        .forEach(searchResult::addMatchingResource);
            }
        }

        return models;
    }

    public SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, YtiUser user) {
        Set<String> allowedDatamodels = new HashSet<>();
        if (!user.isSuperuser()) {
            var organizations = groupManagementService.getOrganizationsForUser(user);
            var modelRequest = new ModelSearchRequest();
            modelRequest.setPageSize(QueryFactoryUtils.INTERNAL_SEARCH_PAGE_SIZE);
            modelRequest.setOrganizations(organizations);
            modelRequest.setIncludeDraftFrom(organizations);
            if (request.getLimitToModelType() != null) {
                modelRequest.setType(Set.of(request.getLimitToModelType()));
            }
            var build = ModelQueryFactory.createModelQuery(modelRequest, user.isSuperuser());
            var response = client.search(build, IndexModel.class);
            allowedDatamodels = response.getResponseObjects().stream()
                    .map(IndexBase::getId)
                    .collect(Collectors.toSet());
        }

        // Add resources from other models to the search request. Used in attribute / association lists
        if (request.getLimitToDataModel() != null
                && !request.isFromAddedNamespaces()
                && request.getResourceTypes() != null) {
            Resource resourceType = null;
            if (request.getResourceTypes().contains(ResourceType.ATTRIBUTE)) {
                resourceType = OWL.DatatypeProperty;
            } else if (request.getResourceTypes().contains(ResourceType.ASSOCIATION)) {
                resourceType = OWL.ObjectProperty;
            }

            if (resourceType != null) {
                request.setAdditionalResources(resourceService.findNodeShapeExternalProperties(
                        request.getLimitToDataModel(), resourceType));
            }
        }
        return searchInternalResources(request, allowedDatamodels, user);
    }

    public SearchResponseDTO<IndexResourceInfo> searchInternalResourcesWithInfo(ResourceSearchRequest request, YtiUser user) {
        var dto = searchInternalResources(request, user);
        var dataModels = new HashMap<String, IndexModel>();

        var modelRequest = new ModelSearchRequest();
        modelRequest.setPageSize(1000);

        searchModels(modelRequest, user).getResponseObjects()
                .forEach(o -> dataModels.put(o.getId(), o));

        var conceptURIs = dto.getResponseObjects().stream()
                .map(IndexResource::getSubject)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var concepts = terminologyService.getConcepts(conceptURIs);
        var response = new SearchResponseDTO<IndexResourceInfo>();
        response.setTotalHitCount(dto.getTotalHitCount());
        response.setResponseObjects(dto.getResponseObjects().stream()
                .map(obj -> ResourceMapper.mapIndexResourceInfo(obj, dataModels, concepts))
                .toList());
        return response;
    }

    public SearchResponseDTO<IndexResource> findResourcesByURI(Set<String> resourceURIs, String versionURI) {
        return client.search(ResourceQueryFactory.createFindResourcesByURIQuery(resourceURIs, versionURI), IndexResource.class);
    }

    private SearchResponseDTO<IndexResource> searchInternalResources(ResourceSearchRequest request, Set<String> allowedDataModels, YtiUser user) {
        var externalNamespaces = new ArrayList<String>();
        var internalNamespaces = new ArrayList<String>();

        if (request.isFromAddedNamespaces()) {
            if (request.getLimitToDataModel() == null) {
                throw new OpenSearchException("limitToDataModel cannot be empty if getting from added namespace", OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE);
            }
            getNamespacesFromModel(request, internalNamespaces, externalNamespaces);
        }

        List<String> restrictedDataModels = null;

        if (request.getLimitToModelType() != null
                || (request.getGroups() != null && !request.getGroups().isEmpty())
                || (request.getStatus() != null && !request.getStatus().isEmpty())) {
            // Skip the search all together if no extra filtering needs to be done
            restrictedDataModels = getModelSpecificRestrictions(request, user);
        }

        // no matched data models found
        if (restrictedDataModels != null && restrictedDataModels.isEmpty()) {
            var result = new SearchResponseDTO<IndexResource>();
            result.setResponseObjects(List.of());
            result.setTotalHitCount(0);
            return result;
        } else {
            var build = ResourceQueryFactory.createInternalResourceQuery(request, externalNamespaces,
                    internalNamespaces, restrictedDataModels, allowedDataModels);
            return client.search(build, IndexResource.class);
        }
    }

    /**
     * Get model specific restrictions for a resource search
     *
     * @return List of DataModel URIs
     */
    private List<String> getModelSpecificRestrictions(ResourceSearchRequest request, YtiUser user) {
        var modelRequest = new ModelSearchRequest();
        modelRequest.setPageSize(QueryFactoryUtils.INTERNAL_SEARCH_PAGE_SIZE);
        if (request.getLimitToModelType() != null) {
            modelRequest.setType(Set.of(request.getLimitToModelType()));
        }
        modelRequest.setStatus(request.getStatus());
        modelRequest.setGroups(request.getGroups());

        if (!user.isSuperuser()) {
            modelRequest.setIncludeDraftFrom(groupManagementService.getOrganizationsForUser(user));
        }
        var query = ModelQueryFactory.createModelQuery(modelRequest, user.isSuperuser());

        var result = client.search(query, IndexModel.class);
        return result.getResponseObjects().stream()
                .map(IndexBase::getId)
                .toList();
    }

    private void getNamespacesFromModel(ResourceSearchRequest request, List<String> internalNamespaces, List<String> externalNamespaces){
        var uri = DataModelURI.fromURI(request.getLimitToDataModel());
        var model = coreRepository.fetch(uri.getGraphURI());
        var resource = model.getResource(uri.getModelURI());
        var allNamespaces = new ArrayList<>(MapperUtils.arrayPropertyToList(resource, OWL.imports));

        if (!ModelType.PROFILE.equals(request.getLimitToModelType())) {
            allNamespaces.addAll(MapperUtils.arrayPropertyToList(resource, DCTerms.requires));

            // resource from external models are searched by isDefinedBy property (include only if searching LIBRARY resources)
            externalNamespaces.addAll(allNamespaces.stream()
                    .filter(ns -> !ns.contains(ModelConstants.SUOMI_FI_DOMAIN)
                                  && !ns.contains(ModelConstants.SUOMI_FI_NAMESPACE))
                    .toList());
        }

        // internal namespaces are searched by versionIRI
        internalNamespaces.addAll(allNamespaces.stream().filter(ns -> ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)).toList());
    }
}
