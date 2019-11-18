package fi.vm.yti.datamodel.api.index;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
    private LuceneQueryFactory luceneQueryFactory;

    @Autowired
    public ResourceQueryFactory(ObjectMapper objectMapper,
                                LuceneQueryFactory luceneQueryFactory) {

        this.objectMapper = objectMapper;
        this.luceneQueryFactory = luceneQueryFactory;

    }

    public SearchRequest createQuery(ResourceSearchRequest request) {
        return createQuery(request.getUri(), request.getQuery(), request.getType(), request.getIsDefinedBy(), request.getIsDefinedBySet(), request.getStatus(), request.getAfter(), request.getBefore(), request.getSortLang(), request.getSortField(), request.getSortOrder(), request.getPageSize(), request.getPageFrom(), request.getFilter());
    }

    private SearchRequest createQuery(Set<String> uris,
                                      String query,
                                      String type,
                                      String modelId,
                                      Set<String> modelSet,
                                      Set<String> status,
                                      Date after,
                                      Date before,
                                      String sortLang,
                                      String sortField,
                                      String sortOrder,
                                      Integer pageSize,
                                      Integer pageFrom,
                                      Set<String> filter) {

        if (sortField != null && !sortField.matches("modified|label|comment|isDefinedBy")) {
            throw new IllegalArgumentException("Allowed fields: modified, label, comment, isDefinedBy");
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        if (pageFrom != null)
            sourceBuilder.from(pageFrom);

        if (pageSize != null) {
            sourceBuilder.size(pageSize);
            if (pageFrom == null) {
                sourceBuilder.from(0);
            }
        } else {
            sourceBuilder.size(10000);
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> mustList = boolQuery.must();

        if(uris!=null) {
            QueryBuilder uriQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.termsQuery("id", uris)).minimumShouldMatch(1);
            mustList.add(uriQuery);
        }

        if (after != null) {
            mustList.add(QueryBuilders.rangeQuery("modified").gte(after));
        }

        if (before != null) {
            mustList.add(QueryBuilders.rangeQuery("modified").lt(before));
        }

        if (filter != null) {
            QueryBuilder filterQuery = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.termsQuery("id", filter));
            mustList.add(filterQuery);
        }

        if (type != null) {
            mustList.add(QueryBuilders.matchQuery("type", type));
        }

        if (modelId != null) {
            mustList.add(QueryBuilders.matchQuery("isDefinedBy", modelId));
        } else if(modelSet != null) {
            QueryBuilder modelSetQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.termsQuery("isDefinedBy", modelSet)).minimumShouldMatch(1);
            mustList.add(modelSetQuery);
        }

        if (status != null) {
            QueryBuilder statusQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.termsQuery("status", status)).minimumShouldMatch(1);
            mustList.add(statusQuery);
        }

        QueryStringQueryBuilder labelQuery = null;

        if (!query.isEmpty()) {
            labelQuery = luceneQueryFactory.buildPrefixSuffixQuery(query).field("label.*");

            if (sortLang != null && sortLangPattern.matcher(sortLang).matches()) {
                labelQuery = labelQuery.field("label." + sortLang, 10);
            }

            mustList.add(labelQuery);
        }

        if (mustList.size() > 0) {
            sourceBuilder.query(boolQuery);
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        if (sortField != null && !sortField.isEmpty() && sortLang != null && !sortLang.isEmpty()) {
            sortOrder = (sortOrder == null ? "desc" : (sortOrder.matches("asc|desc") ? sortOrder : "desc"));
            FieldSortBuilder fieldSort = new FieldSortBuilder(sortField + (sortField.equals("label") || sortField.equals("comment") ? "." + sortLang : "")).order(SortOrder.fromString(sortOrder));
            fieldSort.missing("_last");
            sourceBuilder.sort(fieldSort);
        }

        SearchRequest sr = new SearchRequest("dm_resources")
            .source(sourceBuilder);

        logger.debug(sr.source().toString());

        return sr;

    }

    public ResourceSearchResponse parseResponse(SearchResponse response,
                                                ResourceSearchRequest request,
                                                boolean highlight) {
        List<IndexResourceDTO> resources = new ArrayList<>();

        ResourceSearchResponse ret = new ResourceSearchResponse(0, request.getPageSize(), request.getPageFrom(), resources);

        try {

            SearchHits hits = response.getHits();
            ret.setTotalHitCount(hits.getTotalHits());

            for (SearchHit hit : hits) {
                IndexResourceDTO res = objectMapper.readValue(hit.getSourceAsString(), IndexResourceDTO.class);
                if (highlight) {
                    res.highlightLabels(request.getQuery());
                }
                resources.add(res);
            }

        } catch (Exception e) {
            logger.error("Cannot parse model query response", e);
        }

        return ret;

    }

}
