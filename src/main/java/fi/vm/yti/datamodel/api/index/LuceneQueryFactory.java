package fi.vm.yti.datamodel.api.index;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;

@Singleton
@Service
public class LuceneQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(LuceneQueryFactory.class);

    // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html
    // TODO: Make unit tests for these.
    protected static final String plainQueryPatternString = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=\\w)-++))+$";
    protected static final String complexQueryPatternString = "^[-+:(){}\\[\\]*?~\"/<>=^&|!\\\\\\w\\s]+$";
    protected static final String asteriskQueryPatternString = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=[\\w*])-++|(?<!\\*)\\*(?=[\\w-])|(?<=[\\w-])\\*(?!\\*)))+$";

    private final Pattern plainQueryPattern = Pattern.compile(plainQueryPatternString, Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern plainSplitter = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern givenQueryPattern;

    @Autowired
    public LuceneQueryFactory(ApplicationProperties properties) {
        if (properties.isAllowComplexElasticQueries()) {
            logger.info("Constructing LuceneQueryFactory allowing complex passed queries");
            givenQueryPattern = Pattern.compile(complexQueryPatternString, Pattern.UNICODE_CHARACTER_CLASS);
        } else {
            logger.info("Constructing LuceneQueryFactory allowing only asterisk to pass");
            givenQueryPattern = Pattern.compile(asteriskQueryPatternString, Pattern.UNICODE_CHARACTER_CLASS);
        }
    }

    public QueryStringQueryBuilder buildPrefixSuffixQuery(final String query) {
        if (query != null) {
            final String trimmed = query.trim();
            if (!query.isEmpty()) {
                String parsedQuery = null;
                if (plainQueryPattern.matcher(trimmed).matches()) {
                    String[] splitQuery = plainSplitter.split(trimmed);
                    if (splitQuery.length == 1) {
                        parsedQuery = trimmed + " OR " + trimmed + "* OR *" + trimmed;
                    } else if (splitQuery.length > 1) {
                        parsedQuery = Arrays.stream(splitQuery).map(q -> "(" + q + " OR " + q + "* OR *" + q + ")").collect(Collectors.joining(" AND "));
                    }
                } else if (givenQueryPattern.matcher(trimmed).matches()) {
                    parsedQuery = trimmed;
                }
                if (parsedQuery != null) {
                    StandardQueryParser parser = new StandardQueryParser();
                    try {
                        parser.setAllowLeadingWildcard(true);
                        return QueryBuilders.queryStringQuery(parser.parse(parsedQuery, "").toString());
                    } catch (QueryNodeException e) {
                        // nop
                    }
                }
            }
        }
        logger.debug("Query string disqualified: '" + query + "'");
        throw new BadRequestException("Invalid query");
    }
}
