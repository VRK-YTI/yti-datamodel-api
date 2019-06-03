package fi.vm.yti.datamodel.api.index;

import javax.inject.Singleton;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.springframework.stereotype.Service;

@Singleton
@Service
public class LuceneQueryFactory {

    public QueryStringQueryBuilder buildPrefixSuffixQuery(String query) {
        if (query.contains("*")) {
            return QueryBuilders.queryStringQuery(query);
        } else if (query.contains(" ")) {
            String[] splittedQry = query.split(" ");
            for (int i = 0; i < splittedQry.length; i++) {
                splittedQry[i] = ("(" + splittedQry[i] + " OR *" + splittedQry[i] + " OR " + splittedQry[i] + "*)");
            }
            return QueryBuilders.queryStringQuery(String.join(" AND ", splittedQry));
        } else {
            return QueryBuilders.queryStringQuery(query + " OR " + query + "* OR *" + query);
        }
    }

}
