package fi.vm.yti.datamodel.api.index;

import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public final class ElasticUtils {

    private ElasticUtils() {
    }

    public static QueryBuilder createStatusAndModelQuery(String modelProperty,
                                                         Set<String> priviledgedModels) {
        // Content must be defined in the priviledgedModel or be in other state than INCOMPLETE
        QueryBuilder statusQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("status", "INCOMPLETE"));
        QueryBuilder privilegeQuery;
        if (priviledgedModels != null && !priviledgedModels.isEmpty()) {
            privilegeQuery = QueryBuilders.boolQuery()
                .should(statusQuery)
                .should(QueryBuilders.termsQuery(modelProperty, priviledgedModels))
                .minimumShouldMatch(1);
        } else {
            privilegeQuery = statusQuery;
        }
        return privilegeQuery;
    }

    public static QueryBuilder filterQuery(String modelProperty,
                                           Set<String> filterList) {
        QueryBuilder privilegeQuery;
        privilegeQuery = QueryBuilders.boolQuery()
            .should(QueryBuilders.termsQuery(modelProperty, filterList))
            .minimumShouldMatch(1);
        return privilegeQuery;
    }

    public static QueryBuilder createStatusAndContributorQuery(Set<String> privilegedOrganizations) {
        // Content must either be in some other state than INCOMPLETE, or the user must match a contributor organization.
        QueryBuilder statusQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("status", "INCOMPLETE"));
        QueryBuilder privilegeQuery;
        if (privilegedOrganizations != null) {
            privilegeQuery = QueryBuilders.boolQuery()
                .should(statusQuery)
                .should(QueryBuilders.termsQuery("contributor", privilegedOrganizations))
                .minimumShouldMatch(1);
        } else {
            privilegeQuery = statusQuery;
        }
        return privilegeQuery;
    }

}
