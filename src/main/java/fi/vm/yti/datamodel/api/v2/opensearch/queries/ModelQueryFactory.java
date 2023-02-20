package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ModelQueryFactory {

    private ModelQueryFactory() {
        //only static functions here
    }

    public static SearchRequest createModelQuery(Set<String> groups) {
        List<Query> queryVariantList = new ArrayList<>();

        if(groups != null && !groups.isEmpty()){
            var groupsQuery = TermsQuery.of(query ->
                    query
                            .field("isPartOf")
                            .terms(terms -> terms.value(
                                    groups.stream()
                                            .map(FieldValue::of)
                                            .toList()
                            )));
            queryVariantList.add(groupsQuery._toQuery());
        }

        var finalQuery = QueryBuilders.bool()
                .must(queryVariantList)
                .build();

        SearchRequest sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .query(finalQuery._toQuery())
                .build();

        logPayload(sr);
        return sr;
    }
}
