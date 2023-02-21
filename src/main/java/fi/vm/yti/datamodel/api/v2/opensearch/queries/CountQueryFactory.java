package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationBuilders;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

@Service
public class CountQueryFactory {

    public SearchRequest createModelQuery() {
        var status = QueryBuilders.bool()
                .mustNot(QueryBuilders.term()
                        .field("status")
                        .value(FieldValue.of(Status.INCOMPLETE.name()))
                        .build()._toQuery())
                .build()._toQuery();

        SearchRequest sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .size(0)
                .query(status)
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

    public CountSearchResponse parseResponse(SearchResponse<?> response) {
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
        Map<String, Long> result = new HashMap<>();
        var aggregation = response.aggregations().get(aggregateName);

        if (aggregation == null) {
            return result;
        }
        aggregation
                .sterms()
                .buckets()
                .array()
                .forEach(bucket -> result.put(bucket.key(), bucket.docCount()));

        return result;
    }

}
