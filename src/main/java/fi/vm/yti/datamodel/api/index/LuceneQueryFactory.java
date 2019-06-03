package fi.vm.yti.datamodel.api.index;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Singleton
@Service
public class LuceneQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(LuceneQueryFactory.class.getName());
    public QueryStringQueryBuilder buildPrefixSuffixQuery(String query) {
        String parsedQuery = null;

        if (!query.contains("*")) {
            if (query.contains(" ")) {
                String[] splittedQry = query.split("\\s+");
                for (int i = 0; i < splittedQry.length; i++) {
                    splittedQry[i] = ("(" + splittedQry[i] + " OR *" + splittedQry[i] + " OR " + splittedQry[i] + "*)");
                }
                parsedQuery = String.join(" AND ", splittedQry);
            } else {
                parsedQuery = query + " OR " + query + "* OR *" + query;
            }
        } else {
            parsedQuery = query;
        }

        StandardQueryParser parser = new StandardQueryParser();
        Query luceneQuery = null;

        try {
            parser.setAllowLeadingWildcard(true);
            luceneQuery = parser.parse(parsedQuery, "");
        } catch (QueryNodeException e) {
            logger.warn("Failed to parse: "+parsedQuery);
            throw new BadRequestException("Invalid query!");
        }

        return QueryBuilders.queryStringQuery(luceneQuery.toString());
    }

}
