package fi.vm.yti.datamodel.api.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.jena.base.Sys;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
    private LuceneQueryFactory luceneQueryFactory;

    @Autowired
    public ModelQueryFactory(ObjectMapper objectMapper,
                             LuceneQueryFactory luceneQueryFactory) {

        this.objectMapper = objectMapper;
        this.luceneQueryFactory = luceneQueryFactory;

    }

    public SearchRequest createQuery(ModelSearchRequest request) {
        return createQuery(request.getQuery(), request.getStatus(), request.getType(), request.getAfter(), Collections.EMPTY_SET, request.getPageSize(), request.getPageFrom(), request.getFilter(), request.getIncludeIncomplete(), request.getIncludeIncompleteFrom());
    }


    public SearchRequest createQuery(ModelSearchRequest request,
                                     Collection<String> additionalModelIds) {
        return createQuery(request.getQuery(), request.getStatus(), request.getType(), request.getAfter(), additionalModelIds, request.getPageSize(), request.getPageFrom(), request.getFilter(), request.getIncludeIncomplete(), request.getIncludeIncompleteFrom());
    }

    private SearchRequest createQuery(String query,
                                      Set<String> status,
                                      String type,
                                      Date after,
                                      Collection<String> additionalModelIds,
                                      Integer pageSize,
                                      Integer pageFrom,
                                      Set<String> filter,
                                      Boolean includeIncomplete,
                                      Set<String> includeIncompleteFrom) {

        QueryStringQueryBuilder labelQuery = null;
        if (!query.isEmpty()) {
            labelQuery = luceneQueryFactory.buildPrefixSuffixQuery(query).field("label.*");
        }

        TermsQueryBuilder idQuery = null;
        if (additionalModelIds != null && !additionalModelIds.isEmpty()) {
            idQuery = QueryBuilders.termsQuery("id", additionalModelIds);
        }

        QueryBuilder contentQuery = null;
        if (idQuery != null && labelQuery != null) {
            contentQuery = QueryBuilders.boolQuery()
                .should(labelQuery)
                .should(idQuery)
                .minimumShouldMatch(1);
        } else if (idQuery != null) {
            contentQuery = idQuery;
        } else if (labelQuery != null) {
            contentQuery = labelQuery;
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> mustList = boolQuery.must();

        if(includeIncomplete==null || includeIncompleteFrom!=null) {
            QueryBuilder incompleteQuery = ElasticUtils.createStatusAndContributorQuery(includeIncompleteFrom);
            mustList.add(incompleteQuery);
        }

        if(after != null) {
            mustList.add(QueryBuilders.rangeQuery("modified").gte(after));
        }

        if(filter != null) {
            QueryBuilder filterQuery = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.termsQuery("id", filter));
            mustList.add(filterQuery);
        }

        if (status != null) {
            QueryBuilder statusQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.termsQuery("status", status)).minimumShouldMatch(1);
            mustList.add(statusQuery);
        }

        if (type != null) {
            mustList.add(QueryBuilders.matchQuery("type", type));
        }

        if (contentQuery != null) {
            mustList.add(contentQuery);
        }

        if (mustList.size()>0) {
            sourceBuilder.query(boolQuery);
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        if (pageFrom != null) {
            sourceBuilder.from(pageFrom);
        }

        if (pageSize != null) {
            sourceBuilder.size(pageSize);
            if (pageFrom == null) {
                sourceBuilder.from(0);
            }
        } else {
            sourceBuilder.size(10000);
        }

        SearchRequest sr = new SearchRequest("dm_models")
            .source(sourceBuilder);

        logger.debug(sr.source().toString());

        return sr;

    }

    public ModelSearchResponse parseResponse(SearchResponse response,
                                             ModelSearchRequest request,
                                             Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHitList) {

        List<IndexModelDTO> models = new ArrayList<>();

        ModelSearchResponse ret = new ModelSearchResponse(0, request.getPageSize(), request.getPageFrom(), models, deepSearchHitList);

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
