package fi.vm.yti.datamodel.api.v2.opensearch.queries;


import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.BaseSearchRequest;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOptionsBuilders;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.*;

import java.util.Collection;

public class QueryFactoryUtils {

    private QueryFactoryUtils() {
        //Utility class
    }

    public static final int DEFAULT_PAGE_FROM = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int INTERNAL_SEARCH_PAGE_SIZE = 10000;
    public static final String DEFAULT_SORT_LANG = "fi";

    public static int pageFrom(BaseSearchRequest request) {
        var pageFrom = request.getPageFrom();
        var pageSize = pageSize(request.getPageSize());

        if (pageFrom == null || pageFrom <= 0) {
            return DEFAULT_PAGE_FROM;
        } else {
            return (pageFrom - 1) * pageSize;
        }
    }

    public static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        } else {
            return pageSize;
        }
    }

    public static String getSortLang(String sortLang) {
        if (sortLang == null || sortLang.isBlank()) {
            return DEFAULT_SORT_LANG;
        } else {
            return sortLang;
        }
    }

    public static SortOptions getLangSortOptions(String sortLang) {
        var builder = SortOptionsBuilders.field()
                .field("label." + QueryFactoryUtils.getSortLang(sortLang) + ".keyword")
                .order(SortOrder.Asc)
                .unmappedType(FieldType.Keyword)
                .build();
        return SortOptions.of(s -> s.field(builder));
    }

    //COMMON QUERIES

    public static Query hideDraftStatusQuery() {
        var termQuery = TermQuery.of(q -> q
                .field("status")
                .value(FieldValue.of(Status.DRAFT.name()))
        )._toQuery();
        return BoolQuery.of(q -> q.mustNot(termQuery))._toQuery();
    }

    public static Query termsQuery(String field, Collection<String> values) {
        return TermsQuery.of(q -> q
                        .field(field)
                        .terms(t -> t
                                .value(values.stream().map(FieldValue::of).toList())))
                ._toQuery();
    }

    public static Query termQuery(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TermQuery.of(q -> q
                        .field(field)
                        .value(FieldValue.of(value)))
                ._toQuery();
    }

    public static Query existsQuery(String field, boolean notExists) {
        var exists = ExistsQuery.of(q -> q.field(field))._toQuery();
        if (notExists) {
            return BoolQuery.of(q -> q.mustNot(exists))._toQuery();
        }
        return exists;
    }

    public static Query labelQuery(String query) {
        var trimmed = query.trim();
        final var qs = trimmed.contains(" ")
                ? String.format("\"%s\"", trimmed)
                : String.format("%s~1 *%s*", trimmed, trimmed);
        return QueryStringQuery.of(q-> q
                .query(qs)
                .fields("label.*")
        )._toQuery();
    }
}
