package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ClassSearchRequest;
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

public class ClassQueryFactory {

    private ClassQueryFactory(){
        //only provides static methods
    }
    public static SearchRequest createInternalClassQuery(ClassSearchRequest request, Set<String> fromNamespaces, Set<String> groupRestrictedNamespaces, Set<String> allowedDatamodels) {
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
            var fromNamespacesQuery = QueryFactoryUtils.termsQuery("isDefinedBy", fromNamespaces.stream().toList());
            must.add(fromNamespacesQuery);
        }

        if(groupRestrictedNamespaces != null && !groupRestrictedNamespaces.isEmpty()){
            var groupRestrictedNamespacesQuery = QueryFactoryUtils.termsQuery("isDefinedBy", groupRestrictedNamespaces.stream().toList());
            must.add(groupRestrictedNamespacesQuery);
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
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_CLASS)
                .query(finalQuery)
                .sort(SortOptions.of(sortOptions -> sortOptions.field(sort)))
                .build();
        logPayload(sr);
        return sr;
    }

}
