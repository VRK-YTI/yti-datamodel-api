package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.script.Script;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.BucketOrder;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.terms.Terms;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CountQueryFactory {

    private static final Logger log = LoggerFactory.getLogger(CountQueryFactory.class);

    public SearchRequest createModelQuery() {
        QueryBuilder withIncompleteHandling = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.termQuery("status", Status.INCOMPLETE.name()));

        SearchRequest sr = new SearchRequest(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .source(new SearchSourceBuilder()
                        .size(0)
                        .query(withIncompleteHandling)
                        .aggregation(createStatusAggregation())
                        .aggregation(createTypeAggregation())
                        .aggregation(createLanguageAggregation())
                        .aggregation(createInformationDomainAggregation())
                        );

        log.debug("Count request: {}", sr);
        return sr;
    }

    private TermsAggregationBuilder createStatusAggregation() {
        var scriptSource = "doc.containsKey('status') ? doc.status : params._source.properties.status[0].value";
        Map<String, Object> params = new HashMap<>(16);
        var script = new Script(
                Script.DEFAULT_SCRIPT_TYPE,
                Script.DEFAULT_SCRIPT_LANG,
                scriptSource,
                params);

        return AggregationBuilders
                .terms("statusagg")
                .size(300)
                .script(script);
    }


    private TermsAggregationBuilder createTypeAggregation() {
        var scriptSource = "doc.containsKey('type') ? doc.type : params._source.properties.type[0].value";
        Map<String, Object> params = new HashMap<>(16);
        var script = new Script(
                Script.DEFAULT_SCRIPT_TYPE,
                Script.DEFAULT_SCRIPT_LANG,
                scriptSource,
                params);

        return AggregationBuilders
                .terms("typeagg")
                .size(300)
                .script(script);
    }

    private TermsAggregationBuilder createLanguageAggregation() {
        String scriptSource = "params._source.language" +
                ".stream()" +
                ".collect(Collectors.toList())";

        var script = new Script(
                Script.DEFAULT_SCRIPT_TYPE,
                Script.DEFAULT_SCRIPT_LANG,
                scriptSource,
                new HashMap<>(16));

        return AggregationBuilders
                .terms("langagg")
                .order(BucketOrder.count(false))
                .size(300)
                .script(script);
    }

    private TermsAggregationBuilder createInformationDomainAggregation() {
        var scriptSource = "params._source.isPartOf";

        Map<String, Object> params = new HashMap<>(16);
        var script = new Script(
                Script.DEFAULT_SCRIPT_TYPE,
                Script.DEFAULT_SCRIPT_LANG,
                scriptSource,
                params);

        return AggregationBuilders
                .terms("infodomainagg")
                .size(300)
                .script(script);
    }

    public CountSearchResponse parseResponse(SearchResponse response) {
        var ret = new CountSearchResponse();
        ret.setTotalHitCount(response.getHits().getTotalHits().value);


        Terms statusAgg = response.getAggregations().get("statusagg");
        var statuses = statusAgg
                .getBuckets()
                .stream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        MultiBucketsAggregation.Bucket::getDocCount));

        Terms typeAgg = response.getAggregations().get("typeagg");
        var types = typeAgg
                .getBuckets()
                .stream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        MultiBucketsAggregation.Bucket::getDocCount));

        Terms infodomainAgg = response.getAggregations().get("infodomainagg");
        var infoDomains = infodomainAgg
                .getBuckets()
                .stream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        MultiBucketsAggregation.Bucket::getDocCount));

        Map<String, Long> languages = new HashMap<>();
        Terms langagg = response.getAggregations().get("langagg");
        if (langagg != null) {
            languages = langagg
                    .getBuckets()
                    .stream()
                    .collect(
                            LinkedHashMap::new,
                            (map, item) -> map.put(
                                    item.getKeyAsString(),
                                    item.getDocCount()
                            ),
                            Map::putAll
                    );
        }

        ret.setCounts(new CountDTO(statuses, languages, types, infoDomains));

        return ret;
    }

}
