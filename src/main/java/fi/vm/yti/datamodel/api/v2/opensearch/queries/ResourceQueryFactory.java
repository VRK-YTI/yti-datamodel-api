package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOptionsBuilders;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ResourceQueryFactory {

    private ResourceQueryFactory(){
        //only provides static methods
    }
    public static SearchRequest createInternalResourceQuery(ResourceSearchRequest request, List<String> fromNamespaces, List<String> restrictedDataModels, Set<String> allowedDatamodels) {
        List<Query> must = new ArrayList<>();
        List<Query> should = new ArrayList<>();


        if(allowedDatamodels != null && !allowedDatamodels.isEmpty()){
            var allowedDatamodelsQuery = QueryFactoryUtils.termsQuery("isDefinedBy", allowedDatamodels.stream().toList());
            should.add(allowedDatamodelsQuery);
        }

        var removeIncompleteStatus = QueryFactoryUtils.hideIncompleteStatusQuery();
        should.add(removeIncompleteStatus);


        if(request.getQuery() != null){
            var labelQuery = QueryFactoryUtils.labelQuery(request.getQuery());
            must.add(labelQuery);
        }

        var statuses = request.getStatus();
        if(statuses != null && !statuses.isEmpty()){
            var statusQuery = QueryFactoryUtils.termsQuery("status", statuses.stream().map(Status::name).toList());
            must.add(statusQuery);
        }

        if(fromNamespaces != null && !fromNamespaces.isEmpty()){
            var fromNamespacesQuery = QueryFactoryUtils.termsQuery("isDefinedBy", fromNamespaces);
            must.add(fromNamespacesQuery);
        }
        //TODO it would be great if we could combine these 2 terms queries.
        //Basically just need to combine the list so that it contains the intersecting strings
        if(restrictedDataModels != null && !restrictedDataModels.isEmpty()){
            var groupRestrictedNamespacesQuery = QueryFactoryUtils.termsQuery("isDefinedBy", restrictedDataModels);
            must.add(groupRestrictedNamespacesQuery);
        }

        var types = request.getResourceTypes();
        if(types != null && !types.isEmpty()){
            var typeQuery = QueryFactoryUtils.termsQuery("resourceType", types.stream().map(ResourceType::name).toList());
            must.add(typeQuery);
        }

        if (request.getTargetClass() != null) {
            var targetClassQuery = QueryFactoryUtils.termQuery("targetClass", request.getTargetClass());
            must.add(targetClassQuery);
        }

        var finalQuery = QueryBuilders.bool()
                .must(must)
                .should(should)
                .build()
                ._toQuery();

        var sortLang = request.getSortLang() != null ? request.getSortLang() : QueryFactoryUtils.DEFAULT_SORT_LANG;
        var sort = SortOptionsBuilders.field()
                .field("label." + sortLang + ".keyword")
                .order(SortOrder.Asc)
                .unmappedType(FieldType.Keyword)
                .build();

        SearchRequest sr = new SearchRequest.Builder()
                .from(QueryFactoryUtils.pageFrom(request.getPageFrom()))
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE)
                .query(finalQuery)
                .sort(SortOptions.of(sortOptions -> sortOptions.field(sort)))
                .build();
        logPayload(sr);
        return sr;
    }

}