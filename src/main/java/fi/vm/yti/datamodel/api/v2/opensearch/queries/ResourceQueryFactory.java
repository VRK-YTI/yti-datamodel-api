package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
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
    public static SearchRequest createInternalResourceQuery(ResourceSearchRequest request, List<String> fromNamespaces, List<String> restrictedDataModels, Set<String> allowedDatamodels) {
        var must = new ArrayList<Query>();
        var should = new ArrayList<Query>();

        if(allowedDatamodels != null && !allowedDatamodels.isEmpty()){
            should.add(QueryFactoryUtils.termsQuery("isDefinedBy", allowedDatamodels));
        }

        should.add(QueryFactoryUtils.hideDraftStatusQuery());

        var query = request.getQuery();
        if(query != null && !query.isBlank()){
            must.add(QueryFactoryUtils.labelQuery(query));
        }

        var fromVersion = request.getFromVersion();
        if(fromVersion != null && !fromVersion.isBlank()){
            must.add(QueryFactoryUtils.termQuery("fromVersion", fromVersion));
        }else{
            must.add(QueryFactoryUtils.existsQuery("fromVersion", true));
        }

        var statuses = request.getStatus();
        if(statuses != null && !statuses.isEmpty()){
            must.add(QueryFactoryUtils.termsQuery("status", statuses.stream().map(Status::name).toList()));
        }

        // intersect allowed data model lists and include possible additional resources (references from other models)
        var isDefinedByCondition = getIsDefinedByCondition(fromNamespaces, restrictedDataModels, request.getLimitToDataModel());
        if (!isDefinedByCondition.isEmpty()) {
            must.add(new BoolQuery.Builder()
                    .should(QueryFactoryUtils.termsQuery("isDefinedBy", isDefinedByCondition))
                    .should(QueryFactoryUtils.termsQuery("id", request.getAdditionalResources() != null
                            ? request.getAdditionalResources()
                            : Collections.emptySet()))
                    .minimumShouldMatch("1")
                    .build().
                    _toQuery()
            );
        }

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
        var hasExternalNamespaces = isDefinedByCondition.stream()
                .anyMatch(ns -> !ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
        indices.add(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE);
        if (hasExternalNamespaces) {
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
     * @param fromNamespaces namespaces added to current model
     * @param restrictedDataModels models to include based on query by model type, status, group etc
     * @return intersection of restricted models
     */
    private static List<String> getIsDefinedByCondition(List<String> fromNamespaces, List<String> restrictedDataModels, String limitToDataModel) {
        List<String> isDefinedByCondition = new ArrayList<>();
        if(fromNamespaces != null && !fromNamespaces.isEmpty()) {
            if (restrictedDataModels.isEmpty()) {
                isDefinedByCondition.addAll(fromNamespaces);
            } else {
                isDefinedByCondition.addAll(
                        fromNamespaces.stream()
                            .filter(ns -> !ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)
                                    || restrictedDataModels.contains(ns))
                            .toList()
                );
            }
        } else {
            isDefinedByCondition.addAll(restrictedDataModels);
        }

        //Limit to datamodel shouldn't be added in the intersection if it's not the right ModelType
        if (limitToDataModel != null && (restrictedDataModels.isEmpty() || restrictedDataModels.contains(limitToDataModel))) {
            isDefinedByCondition.add(limitToDataModel);
        }
        return isDefinedByCondition;
    }

}
