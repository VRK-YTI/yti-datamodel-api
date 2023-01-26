package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.v2.elasticsearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.elasticsearch.queries.CountQueryFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchIndexService {

    private final CountQueryFactory countQueryFactory;
    private final RestHighLevelClient client;

    public SearchIndexService(ElasticConnector elasticConnector, CountQueryFactory countQueryFactory) {
        this.countQueryFactory = countQueryFactory;
        this.client = elasticConnector.getEsClient();
    }

    /**
     * List counts of data model grouped by different search results
     * @return response containing counts for data models
     */
    public CountSearchResponse getCounts() {
        SearchRequest query = countQueryFactory.createModelQuery();
        try {
            SearchResponse response = client.search(query, RequestOptions.DEFAULT);
            return countQueryFactory.parseResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
