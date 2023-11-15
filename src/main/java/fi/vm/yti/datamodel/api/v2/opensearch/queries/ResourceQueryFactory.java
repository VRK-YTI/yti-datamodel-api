package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ResourceQueryFactory {

    private ResourceQueryFactory(){
        //only provides static methods
    }
    public static SearchRequest createInternalResourceQuery(ResourceSearchRequest request,
                                                            List<String> externalNamespaces,
                                                            List<String> internalNamespaces,
                                                            List<String> restrictedDataModels,
                                                            Set<String> allowedDataModels) {
        var must = new ArrayList<Query>();
        var should = new ArrayList<Query>();

        if(allowedDataModels != null && !allowedDataModels.isEmpty()){
            should.add(QueryFactoryUtils.termsQuery("isDefinedBy", allowedDataModels));
        }

        should.add(QueryFactoryUtils.hideDraftStatusQuery());

        var query = request.getQuery();
        if(query != null && !query.isBlank()){
            must.add(QueryFactoryUtils.labelQuery(query));
        }

        must.add(getIncludedNamespacesQuery(request, externalNamespaces, internalNamespaces, restrictedDataModels));


        var types = request.getResourceTypes();
        if(types != null && !types.isEmpty()){
            must.add(QueryFactoryUtils.termsQuery("resourceType", types.stream().map(ResourceType::name).toList()));
        }

        if (request.getTargetClass() != null) {
            must.add(QueryFactoryUtils.termQuery("targetClass", request.getTargetClass()));
        }

        var finalQuery = QueryBuilders.bool()
                .must(must)
                .should(should)
                .build()
                ._toQuery();

        var indices = new ArrayList<String>();
        indices.add(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE);

        // include external models only if there are no Interoperability platform specific conditions
        if (!externalNamespaces.isEmpty() && request.getGroups() == null) {
            indices.add(OpenSearchIndexer.OPEN_SEARCH_INDEX_EXTERNAL);
        }

        SearchRequest sr = new SearchRequest.Builder()
                .from(QueryFactoryUtils.pageFrom(request.getPageFrom()))
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .index(indices)
                .query(finalQuery)
                .sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()))
                .build();
        logPayload(sr, String.join(", ", indices));
        return sr;
    }

    private static Query getIncludedNamespacesQuery(ResourceSearchRequest request, List<String> externalNamespaces, List<String> internalNamespaces, List<String> restrictedDataModels) {
        // filter out restricted models from included internal namespaces
        var internalNamespacesCondition = getInternalNamespaceCondition(request.isFromAddedNamespaces(), internalNamespaces, restrictedDataModels);

        var builder = new BoolQuery.Builder();
        builder.minimumShouldMatch("1");

        if (restrictedDataModels == null || restrictedDataModels.contains(request.getLimitToDataModel())) {
            builder.should(addCurrentModelQuery(request));
        }

        if(request.getIncludeDraftFrom() != null && !request.getIncludeDraftFrom().isEmpty()) {
            builder.should(QueryFactoryUtils.termsQuery("isDefinedBy", request.getIncludeDraftFrom()));
        }

        builder.should(QueryFactoryUtils.termsQuery("isDefinedBy", externalNamespaces))
            .should(QueryFactoryUtils.termsQuery("versionIri", internalNamespacesCondition))
            .should(QueryFactoryUtils.termsQuery("id", request.getAdditionalResources() != null
                    ? request.getAdditionalResources()
                    : Collections.emptyList()));
        return builder.build()._toQuery();
    }

    /**
     * limit to particular model with or without version in case if not included to restricted data models
     */
    private static Query addCurrentModelQuery(ResourceSearchRequest request) {
        var fromVersion = request.getFromVersion();
        BoolQuery.Builder builder = new BoolQuery.Builder();

        builder.must(QueryFactoryUtils.termsQuery("isDefinedBy", List.of(request.getLimitToDataModel())));
        if (fromVersion != null && !fromVersion.isBlank()) {
            builder.must(QueryFactoryUtils.termQuery("fromVersion", fromVersion));
        } else {
            builder.must(QueryFactoryUtils.existsQuery("fromVersion", true));
        }
        return builder.build()._toQuery();
    }

    public static SearchRequest createFindResourcesByURIQuery(Set<String> resourceURIs) {
        return new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE)
                .query(QueryBuilders
                        .bool()
                        .must(QueryFactoryUtils.termsQuery("id", resourceURIs))
                        .build()
                        ._toQuery())
                .build();
    }

    /**
     * Intersect allowed data model lists. Only checks models from Interoperability platform,
     * external namespaces are always included.
     * @param internalNamespaces internal namespaces added to current model
     * @param restrictedDataModels models to include based on query by model type, status, group etc
     * @return intersection of restricted models
     */
    private static List<String> getInternalNamespaceCondition(boolean isFromAddedNamespaces, List<String> internalNamespaces, List<String> restrictedDataModels) {
        //no need to intersect if other one is null
        if(restrictedDataModels == null) {
            return internalNamespaces;
        }

        //internalNamespaces cannot be null so check return restrictedModels if its empty
        if(!isFromAddedNamespaces && internalNamespaces.isEmpty()) {
            return restrictedDataModels;
        }

        return internalNamespaces.stream()
                .filter(restrictedDataModels::contains)
                .toList();
    }

}
