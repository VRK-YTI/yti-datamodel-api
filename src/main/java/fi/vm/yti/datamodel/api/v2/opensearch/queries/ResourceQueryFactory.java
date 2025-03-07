package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.service.IndexService;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Highlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

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
                .toQuery();

        var indices = new ArrayList<String>();
        indices.add(IndexService.OPEN_SEARCH_INDEX_RESOURCE);

        // include external models only if there are no Interoperability platform specific conditions
        if (!externalNamespaces.isEmpty() && request.getGroups() == null) {
            indices.add(IndexService.OPEN_SEARCH_INDEX_EXTERNAL);
        }

        var builder = new SearchRequest.Builder()
                .from(QueryFactoryUtils.pageFrom(request))
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .index(indices)
                .query(finalQuery);

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            builder.sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()));
        }
        var sr = builder.build();

        logPayload(sr, String.join(", ", indices));
        return sr;
    }

    public static SearchRequest createResourceQuery(ResourceSearchRequest request,
                                                    List<String> externalNamespaces) {
        var must = new ArrayList<Query>();
        var should = new ArrayList<Query>();

        should.add(QueryFactoryUtils.hideDraftStatusQuery());

        var query = request.getQuery();
        if (query != null && !query.isBlank()){
            must.add(QueryFactoryUtils.labelQuery(query));
        }

        var types = request.getResourceTypes();
        if (types != null && !types.isEmpty()) {
            must.add(QueryFactoryUtils.termsQuery("resourceType", types.stream().map(ResourceType::name).toList()));
        }

        if (request.getTargetClass() != null) {
            must.add(QueryFactoryUtils.termQuery("targetClass", request.getTargetClass()));
        }

        var finalQuery = QueryBuilders.bool()
                .must(must)
                .should(should)
                .build()
                .toQuery();

        var indices = new ArrayList<String>();
        indices.add(IndexService.OPEN_SEARCH_INDEX_RESOURCE);

        // include external models only if there are no Interoperability platform specific conditions
        if (!externalNamespaces.isEmpty() && request.getGroups() == null) {
            indices.add(IndexService.OPEN_SEARCH_INDEX_EXTERNAL);
        }

        Highlight.Builder highlight = new Highlight.Builder()
                .fields("label.*", f -> f)
                .preTags("<b>")
                .postTags("</b>");
        var builder = new SearchRequest.Builder()
                .from(QueryFactoryUtils.pageFrom(request))
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .index(indices)
                .query(finalQuery)
                .highlight(highlight.build());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            builder.sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()));
        }

        var sr = builder.build();
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

        var linkedDraftModels = new ArrayList<String>();

        // Add draft models to the separate list. Draft models do not have version,
        // published model's URI ends with version, e.g. /model/test-prefix/1.2.3/
        internalNamespacesCondition.stream()
                .filter(ns -> !ns.matches("(.*)\\.\\d+/$"))
                .forEach(linkedDraftModels::add);

        if (!linkedDraftModels.isEmpty()) {
            BoolQuery.Builder draftBuilder = new BoolQuery.Builder();
            draftBuilder.must(QueryFactoryUtils.termsQuery("isDefinedBy", linkedDraftModels));
            draftBuilder.must(QueryFactoryUtils.existsQuery("fromVersion", true));
            builder.should(draftBuilder.build().toQuery());
        }

        if (request.getIncludeDraftFrom() != null && !request.getIncludeDraftFrom().isEmpty()) {
            builder.should(QueryFactoryUtils.termsQuery("isDefinedBy", request.getIncludeDraftFrom()));
        }

        builder.should(QueryFactoryUtils.termsQuery("namespace", externalNamespaces))
            .should(QueryFactoryUtils.termsQuery("versionIri", internalNamespacesCondition))
            .should(QueryFactoryUtils.termsQuery("id", request.getAdditionalResources() != null
                    ? request.getAdditionalResources()
                    : Collections.emptyList()));
        return builder.build().toQuery();
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
        return builder.build().toQuery();
    }

    public static SearchRequest createFindResourcesByURIQuery(Set<String> resourceURIs, String versionURI) {
        var queries = new ArrayList<Query>();
        var field = versionURI == null ? "id" : "uri";
        queries.add(QueryFactoryUtils.termsQuery(field, resourceURIs));

        if (versionURI != null) {
            queries.add(ExistsQuery.of(q -> q.field("versionIri")).toQuery());
        }

        var req = new SearchRequest.Builder()
                .index(IndexService.OPEN_SEARCH_INDEX_RESOURCE, IndexService.OPEN_SEARCH_INDEX_EXTERNAL)
                .size(1000)
                .query(QueryBuilders
                        .bool()
                        .must(queries)
                        .build()
                        .toQuery())
                .build();
        logPayload(req, "resources_v2");
        return req;
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
