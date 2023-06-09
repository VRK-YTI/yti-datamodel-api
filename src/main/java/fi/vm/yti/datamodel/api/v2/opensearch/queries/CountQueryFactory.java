package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationBuilders;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;
public class CountQueryFactory {

    private CountQueryFactory(){
        //Static class
    }
    public static SearchRequest createModelQuery() {
        var sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .size(0)
                .query(QueryFactoryUtils.hideIncompleteStatusQuery())
                .aggregations("statuses", getAggregation("status"))
                .aggregations("types", getAggregation("type"))
                .aggregations("languages", getAggregation("language"))
                .aggregations("groups", getAggregation("isPartOf"))
                .build();

        logPayload(sr);
        return sr;
    }

    private static Aggregation getAggregation(String fieldName) {
        return AggregationBuilders.terms().field(fieldName).build()._toAggregation();
    }

    public static CountSearchResponse parseResponse(SearchResponse<?> response) {
        var ret = new CountSearchResponse();
        var counts = new CountDTO(
                getBucketValues(response, "statuses"),
                getBucketValues(response, "languages"),
                getBucketValues(response, "types"),
                getBucketValues(response, "groups")
        );
        ret.setTotalHitCount(response.hits().total().value());
        ret.setCounts(counts);
        return ret;
    }

    private static Map<String, Long> getBucketValues(SearchResponse<?> response, String aggregateName) {
        var aggregation = response.aggregations().get(aggregateName);

        if (aggregation == null) {
            return Collections.emptyMap();
        }
        return aggregation
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount));
    }

}
