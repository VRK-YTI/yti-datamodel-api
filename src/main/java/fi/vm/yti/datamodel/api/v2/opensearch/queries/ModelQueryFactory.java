package fi.vm.yti.datamodel.api.v2.opensearch.queries;

import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationBuilders;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

public class ModelQueryFactory {

    private ModelQueryFactory() {
        //only static functions here
    }

    public static SearchRequest createModelQuery(ModelSearchRequest request, boolean isSuperUser) {
        var modelQuery = getModelBaseQuery(request, isSuperUser);

        Highlight.Builder highlight = new Highlight.Builder();
        highlight.fields("label.*", f -> f);
        var sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .from(QueryFactoryUtils.pageFrom(request))
                .sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()))
                .highlight(highlight.build())
                .query(modelQuery)
                .build();

        logPayload(sr, OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        return sr;
    }

    public static SearchRequest createModelCountQuery(ModelSearchRequest request, boolean isSuperUser) {
        var modelQuery = getModelBaseQuery(request, isSuperUser);

        var sr = new SearchRequest.Builder()
                .index(OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL)
                .size(0)
                .query(modelQuery)
                .aggregations("statuses", getAggregation("status"))
                .aggregations("types", getAggregation("type"))
                .aggregations("languages", getAggregation("language"))
                .aggregations("groups", getAggregation("isPartOf"))
                .build();

        logPayload(sr, OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL);
        return sr;
    }

    public static CountSearchResponse parseModelCountResponse(SearchResponse<?> response) {
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

    private static Query getModelBaseQuery(ModelSearchRequest request, boolean isSuperUser) {
        var allQueries = new ArrayList<Query>();

        // match to data model label OR match to resource query done earlier (additionalModelIds)
        var textQueries = new ArrayList<Query>();

        // status is not draft OR organization is some of user's organizations
        var excludeDrafts = new ArrayList<Query>();

        var draftFrom = request.getIncludeDraftFrom();
        if (draftFrom != null && !draftFrom.isEmpty()) {
            var incompleteFromQuery = QueryFactoryUtils.termsQuery("contributor", draftFrom.stream().map(UUID::toString).toList());
            excludeDrafts.add(incompleteFromQuery);
        }

        if (!isSuperUser) {
            excludeDrafts.add(QueryFactoryUtils.hideDraftStatusQuery());
        }

        var modelType = request.getType();
        if (modelType != null && !modelType.isEmpty()) {
            var modelTypeQuery = QueryFactoryUtils.termsQuery("type", modelType.stream().map(ModelType::name).toList());
            allQueries.add(modelTypeQuery);
        }

        var groups = request.getGroups();
        if (groups != null && !groups.isEmpty()) {
            var groupsQuery = QueryFactoryUtils.termsQuery("isPartOf", groups);
            allQueries.add(groupsQuery);
        }

        var organizations = request.getOrganizations();
        if (organizations != null && !organizations.isEmpty()) {
            var orgsQuery = QueryFactoryUtils.termsQuery("contributor", organizations.stream().map(UUID::toString).toList());
            allQueries.add(orgsQuery);
        }

        var language = request.getLanguage();
        if (language != null && !language.isBlank()) {
            var languageQuery = QueryFactoryUtils.termQuery("language", language);
            allQueries.add(languageQuery);
        }

        var status = request.getStatus();
        if (status != null && !status.isEmpty()) {
            var statusQuery = QueryFactoryUtils.termsQuery("status", status.stream().map(Status::name).toList());
            allQueries.add(statusQuery);
        }

        var additionalModelIds = request.getAdditionalModelIds();
        Query additionalModelIdsQuery = null;
        if (additionalModelIds != null && !additionalModelIds.isEmpty()) {
            additionalModelIdsQuery = QueryFactoryUtils.termsQuery("uri", additionalModelIds);
        }

        if (additionalModelIdsQuery != null) {
            textQueries.add(additionalModelIdsQuery);
        }

        var queryString = request.getQuery();
        if (queryString != null && !queryString.isBlank()) {
            textQueries.add(QueryFactoryUtils.labelQuery(queryString));
        }

        if (excludeDrafts.size() == 1) {
            allQueries.add(excludeDrafts.get(0));
        } else if (excludeDrafts.size() > 1) {
            allQueries.add(QueryBuilders.bool()
                    .should(excludeDrafts)
                    .minimumShouldMatch("1")
                    .build()
                    ._toQuery());
        }

        if (textQueries.size() == 1) {
            // no need to build a "should" query from a single expression
            allQueries.add(textQueries.get(0));
        } else if (textQueries.size() > 1) {
            // build a "should" query from all entries and add to the "must"
            allQueries.add(QueryBuilders.bool()
                    .should(textQueries)
                    .minimumShouldMatch("1")
                    .build()
                    ._toQuery());
        }

        return QueryBuilders.bool().must(allQueries).build()._toQuery();
    }

    private static Aggregation getAggregation(String fieldName) {
        return AggregationBuilders.terms().field(fieldName).build()._toAggregation();
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
