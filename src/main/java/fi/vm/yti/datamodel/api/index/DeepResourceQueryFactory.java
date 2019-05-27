package fi.vm.yti.datamodel.api.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.model.DeepSearchResourceHitListDTO;
import fi.vm.yti.datamodel.api.index.model.DeepSearchHitListDTO;
import fi.vm.yti.datamodel.api.index.model.IndexResourceDTO;

@Singleton
@Service
public class DeepResourceQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeepResourceQueryFactory.class);
    private ObjectMapper objectMapper;
    private static final Pattern prefLangPattern = Pattern.compile("[a-zA-Z-]+");
    private static final FetchSourceContext sourceIncludes = new FetchSourceContext(true, new String[]{ "id", "status", "label", "comment", "isDefinedBy", "type" }, new String[]{});
    private static final Script topHitScript = new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "_score", Collections.emptyMap());

    @Autowired
    public DeepResourceQueryFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchRequest createQuery(String query,
                                     String prefLang) {

        MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(query, "label.*")
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
            .minimumShouldMatch("90%");
        if (prefLang != null && prefLangPattern.matcher(prefLang).matches()) {
            multiMatch = multiMatch.field("label." + prefLang, 10);
        }

        SearchRequest sr = new SearchRequest("dm_resources")
            .source(new SearchSourceBuilder()
                .query(multiMatch)
                .size(0)
                .aggregation(AggregationBuilders.terms("group_by_model")
                    .field("isDefinedBy")
                    .size(1000)
                    .order(BucketOrder.aggregation("best_class_hit", false))
                    .subAggregation(AggregationBuilders.topHits("top_resource_hits")
                        .sort(SortBuilders.scoreSort().order(SortOrder.DESC))
                        .size(6)
                        .fetchSource(sourceIncludes)
                        .highlighter(new HighlightBuilder().preTags("<b>").postTags("</b>").field("label.*")))
                    .subAggregation(AggregationBuilders.max("best_class_hit")
                        .script(topHitScript))));
        return sr;
    }

    public Map<String, List<DeepSearchHitListDTO<?>>> parseResponse(SearchResponse response) {
        Map<String, List<DeepSearchHitListDTO<?>>> ret = new HashMap<>();
        try {
            Terms groupBy = response.getAggregations().get("group_by_model");
            for (Terms.Bucket bucket : groupBy.getBuckets()) {
                TopHits hitsAggr = bucket.getAggregations().get("top_resource_hits");
                SearchHits hits = hitsAggr.getHits();

                long total = hits.getTotalHits();
                if (total > 0) {
                    String modelId = bucket.getKeyAsString();
                    List<IndexResourceDTO> topHits = new ArrayList<>();
                    DeepSearchResourceHitListDTO hitList = new DeepSearchResourceHitListDTO(total, topHits);
                    ret.put(modelId, Collections.singletonList(hitList));

                    for (SearchHit hit : hits.getHits()) {
                        IndexResourceDTO indexResource = objectMapper.readValue(hit.getSourceAsString(), IndexResourceDTO.class);
                        topHits.add(indexResource);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Cannot parse deep class query response", e);
        }
        return ret;
    }
}