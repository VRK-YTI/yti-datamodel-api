package fi.vm.yti.datamodel.api.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.model.DeepSearchHitListDTO;
import fi.vm.yti.datamodel.api.index.model.IndexModelDTO;
import fi.vm.yti.datamodel.api.index.model.ModelSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ModelSearchResponse;

@Singleton
@Service
public class ModelQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(ModelQueryFactory.class);
    private ObjectMapper objectMapper;

    @Autowired
    public ModelQueryFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

    }

    public SearchRequest createQuery(ModelSearchRequest request) {
        return createQuery(request.getQuery(), Collections.EMPTY_SET, request.getPageSize(), request.getPageFrom());
    }

    public SearchRequest createQuery(ModelSearchRequest request,
                                     Collection<String> additionalModelIds) {
        return createQuery(request.getQuery(), additionalModelIds, request.getPageSize(), request.getPageFrom());
    }

    private SearchRequest createQuery(String query,
                                      Collection<String> additionalModelIds,
                                      Integer pageSize,
                                      Integer pageFrom) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        if (pageFrom != null)
            sourceBuilder.from(pageFrom);

        if (pageSize != null)
            sourceBuilder.size(pageSize);

        QueryStringQueryBuilder labelQuery = null;

        if (!query.isEmpty()) {
            labelQuery = QueryBuilders.queryStringQuery(query+" OR "+query+"* OR *"+query).field("label.*");
        }

        TermsQueryBuilder idQuery = null;

        if (additionalModelIds != null && !additionalModelIds.isEmpty()) {

            idQuery = QueryBuilders.termsQuery("id.keyword", additionalModelIds);

        }

        if (idQuery != null && labelQuery != null) {

            sourceBuilder.query(QueryBuilders.boolQuery()
                .should(labelQuery)
                .should(idQuery)
                .minimumShouldMatch(1));

        } else if (idQuery != null) {
            sourceBuilder.query(idQuery);
        } else if (labelQuery != null) {
            sourceBuilder.query(labelQuery);
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        logger.debug(sourceBuilder.toString());

        SearchRequest sr = new SearchRequest("dm_models")
            .source(sourceBuilder);

        return sr;

    }

    public ModelSearchResponse parseResponse(SearchResponse response,
                                             ModelSearchRequest request,
                                             Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHitList) {
        List<IndexModelDTO> models = new ArrayList<>();

        ModelSearchResponse ret = new ModelSearchResponse(0, request.getPageFrom(), models, deepSearchHitList);

        try {

            SearchHits hits = response.getHits();
            ret.setTotalHitCount(hits.getTotalHits());

            for (SearchHit hit : hits) {
                IndexModelDTO model = objectMapper.readValue(hit.getSourceAsString(), IndexModelDTO.class);
                models.add(model);
            }

        } catch (Exception e) {
            logger.error("Cannot parse model query response", e);
        }

        return ret;

    }

}