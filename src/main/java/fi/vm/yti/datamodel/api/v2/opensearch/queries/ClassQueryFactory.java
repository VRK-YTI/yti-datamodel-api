package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.opensearch.dto.ClassSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOptionsBuilders;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ClassQueryFactory {

    private ClassQueryFactory(){
        //only provides static methods
    }
    public static SearchRequest createInternalClassQuery(ClassSearchRequest request, Set<String> fromNamespaces, Set<String> groupRestrictedNamespaces) {
        List<Query> queryVariantList = new ArrayList<>();

        if(request.getQuery() != null){
            var query = request.getQuery();
            var labelQuery = QueryBuilders.queryString()
                    .query("*" + query + "*")
                    .fields("label.*")
                    .fuzziness("2")
                    .build();
            queryVariantList.add(labelQuery._toQuery());
        }

        var statuses = request.getStatus();
        if(statuses != null && !statuses.isEmpty()){
            var statusQuery = TermsQuery.of(query ->
                    query
                        .field("status")
                        .terms(terms -> terms.value(
                            statuses.stream()
                                    .map(status -> FieldValue.of(status.name()))
                                    .toList()
                            ))
            );
            queryVariantList.add(statusQuery._toQuery());
        }

        if(fromNamespaces != null && !fromNamespaces.isEmpty()){
            var fromNamespacesQuery = TermsQuery.of(query ->
                    query
                        .field("isDefinedBy")
                        .terms(terms -> terms.value(
                                fromNamespaces.stream()
                                            .map(FieldValue::of)
                                            .toList()
                            )));
            queryVariantList.add(fromNamespacesQuery._toQuery());
        }

        if(groupRestrictedNamespaces != null && !groupRestrictedNamespaces.isEmpty()){
            var groupRestrictedNamespacesQuery = TermsQuery.of(query ->
                    query
                       .field("isDefinedBy")
                    .terms(terms -> terms.value(
                            groupRestrictedNamespaces.stream()
                                    .map(FieldValue::of)
                                    .toList()
                    )));
            queryVariantList.add(groupRestrictedNamespacesQuery._toQuery());
        }

        var finalQuery = queryVariantList.isEmpty() ? null : QueryBuilders.bool()
                .must(queryVariantList)
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
