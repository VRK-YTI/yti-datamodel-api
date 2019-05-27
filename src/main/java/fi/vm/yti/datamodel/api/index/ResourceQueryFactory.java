package fi.vm.yti.datamodel.api.index;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.model.IndexResourceDTO;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ResourceSearchResponse;

@Singleton
@Service
public class ResourceQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResourceQueryFactory.class);
    private static final Pattern sortLangPattern = Pattern.compile("[a-zA-Z-]+");
    private ObjectMapper objectMapper;

    @Autowired
    public ResourceQueryFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

    }

    public SearchRequest createQuery(ResourceSearchRequest request) {
        return createQuery(request.getQuery(), request.getType(), request.getIsDefinedBy(), request.getSortLang(), request.getSortField(), request.getSortOrder(), request.getPageSize(), request.getPageFrom());
    }

    private SearchRequest createQuery(String query,
                                      String type,
                                      String modelId,
                                      String sortLang,
                                      String sortField,
                                      String sortOrder,
                                      Integer pageSize,
                                      Integer pageFrom) {

        //TODO: Validate sortOrder: asc||desc ... and sortField: modified || label || comment || isDefinedBy etc?

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        if (pageFrom != null)
            sourceBuilder.from(pageFrom);

        if (pageSize != null)
            sourceBuilder.size(pageSize);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> mustList = mustList = boolQuery.must();

        if (type != null) {
            mustList.add(QueryBuilders.matchQuery("type", type).operator(Operator.AND));
        }
        if (modelId != null) {
            mustList.add(QueryBuilders.matchQuery("isDefinedBy", modelId).operator(Operator.AND));
        }

        MultiMatchQueryBuilder labelQuery = null;

        if (!query.isEmpty()) {
            labelQuery = QueryBuilders.multiMatchQuery(query,"label.*")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);

            if (sortLang != null && sortLangPattern.matcher(sortLang).matches()) {
                labelQuery = labelQuery.field("label." + sortLang, 10);
            }

            if (sortLang != null && sortLangPattern.matcher(sortLang).matches()) {
                labelQuery.field("label." + sortLang, 10);
            }

            sourceBuilder.highlighter(new HighlightBuilder().preTags("<b>").postTags("</b>").field("label.*"));
            mustList.add(labelQuery.operator(Operator.AND));
        }

        if ((query!=null && !query.isEmpty()) || type!=null || modelId!=null) {
            sourceBuilder.query(boolQuery);
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        if(sortField!=null && !sortField.isEmpty() && sortLang!=null && !sortLang.isEmpty()) {
            if(sortOrder==null || sortOrder!=null && sortOrder.equals("undefined") || sortOrder!=null && sortOrder.isEmpty()) sortOrder = "desc";
            FieldSortBuilder fieldSort = new FieldSortBuilder(sortField+(sortField.equals("label")||sortField.equals("comment") ? "."+sortLang : "")).order(SortOrder.fromString(sortOrder));
            fieldSort.missing("_last");
            sourceBuilder.sort(fieldSort);
        }

        SearchRequest sr = new SearchRequest("dm_resources")
            .source(sourceBuilder);

        return sr;

    }

    public ResourceSearchResponse parseResponse(SearchResponse response,
                                                ResourceSearchRequest request) {
        List<IndexResourceDTO> resources = new ArrayList<>();

        ResourceSearchResponse ret = new ResourceSearchResponse(0, request.getPageFrom(), resources);

        try {

            SearchHits hits = response.getHits();
            ret.setTotalHitCount(hits.getTotalHits());

            for (SearchHit hit : hits) {
                IndexResourceDTO res = objectMapper.readValue(hit.getSourceAsString(), IndexResourceDTO.class);
                resources.add(res);
            }

        } catch (Exception e) {
            logger.error("Cannot parse model query response", e);
        }

        return ret;

    }

}