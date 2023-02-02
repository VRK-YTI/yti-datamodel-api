package fi.vm.yti.datamodel.api.index;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ContextParser;
import org.opensearch.common.xcontent.DeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.StringTerms;
import org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.opensearch.search.aggregations.metrics.ParsedMax;
import org.opensearch.search.aggregations.metrics.ParsedTopHits;
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EsUtils {

    public static String getJsonString(String file) throws Exception {
        return new String(EsUtils.class
                .getResourceAsStream(file).readAllBytes(), StandardCharsets.UTF_8);
    }

    public static SearchResponse getMockResponse(String path) throws Exception {
        return getSearchResponseFromJson(getJsonString(path));
    }

    // for use with getSearchResponseFromJson
    private static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        // Elasticsearch needs a hint to know what type of aggregation to
        // parse this as. The hint is provided by elastic when
        // adding ?typed_keys to the query.
        // e.g. "sterms#group_by_terminology"
        map.put(TopHitsAggregationBuilder.NAME, (p, c) ->
                ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) ->
                ParsedStringTerms.fromXContent(p, (String) c));
        map.put(MaxAggregationBuilder.NAME, (p, c) ->
                ParsedMax.fromXContent(p, (String) c));
        return map.entrySet().stream()
                .map(entry -> new NamedXContentRegistry.Entry(
                        Aggregation.class,
                        new ParseField(entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.toList());
    }

    // helper method for generating elasticsearch SearchResponse from JSON
    private static SearchResponse getSearchResponseFromJson(String jsonResponse) throws IOException {
        NamedXContentRegistry registry = new NamedXContentRegistry(
                getDefaultNamedXContents());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                registry,
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                jsonResponse);
        SearchResponse searchResponse = SearchResponse.fromXContent(parser);
        return searchResponse;
    }
}
