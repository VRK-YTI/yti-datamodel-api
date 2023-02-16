package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.opensearch.dto.ClassSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.List;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ClassQueryFactory {

    private ClassQueryFactory(){
        //only provides static methods
    }

    public static SearchRequest createClassQuery(ClassSearchRequest request) {
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

        var finalQuery = QueryBuilders.bool()
                .must(queryVariantList)
                .build();

        SearchRequest sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_CLASS)
                .query(finalQuery._toQuery())
                .build();

        logPayload(sr);
        return sr;
    }

}
