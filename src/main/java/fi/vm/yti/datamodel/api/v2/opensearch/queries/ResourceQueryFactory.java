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
            should.add(QueryFactoryUtils.termsQuery("isDefinedBy", allowedDatamodels));
        }

        should.add(QueryFactoryUtils.hideIncompleteStatusQuery());

        var query = request.getQuery();
        if(query != null && !query.isBlank()){
            must.add(QueryFactoryUtils.labelQuery(query));
        }

        var statuses = request.getStatus();
        if(statuses != null && !statuses.isEmpty()){
            must.add(QueryFactoryUtils.termsQuery("status", statuses.stream().map(Status::name).toList()));
        }

        if(fromNamespaces != null && !fromNamespaces.isEmpty()){
            must.add(QueryFactoryUtils.termsQuery("isDefinedBy", fromNamespaces));
        }
        //TODO it would be great if we could combine these 2 terms queries.
        //Basically just need to combine the list so that it contains the intersecting strings
        if(restrictedDataModels != null && !restrictedDataModels.isEmpty()){
            must.add(QueryFactoryUtils.termsQuery("isDefinedBy", restrictedDataModels));
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
