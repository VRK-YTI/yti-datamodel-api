package fi.vm.yti.datamodel.api.v2.elasticsearch;

import fi.vm.yti.datamodel.api.index.EsUtils;
import fi.vm.yti.datamodel.api.v2.elasticsearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.elasticsearch.queries.CountQueryFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CountQueryFactoryTest {

    CountQueryFactory factory = new CountQueryFactory();

    @Test
    void testModelCounts() throws Exception {
        String expected = EsUtils.getJsonString("/es/models_count_request.json");

        SearchRequest request = factory.createModelQuery();

        JSONAssert.assertEquals(expected, request.source().toString(), JSONCompareMode.LENIENT);
    }

    @Test
    void testParseModelCountResponse() throws Exception {
        SearchResponse response = EsUtils.getMockResponse("/es/models_count_response.json");

        CountSearchResponse countSearchResponse = factory.parseResponse(response);

        assertEquals(8, countSearchResponse.getTotalHitCount());
        Map<String, Long> groups = countSearchResponse.getCounts().getGroups();
        Map<String, Long> statuses = countSearchResponse.getCounts().getStatuses();

        assertEquals(3, groups.keySet().size());
        assertEquals(2L, groups.get("P13"));
        assertEquals(1L, groups.get("P11"));
        assertEquals(1L, groups.get("P21"));

        assertEquals(2, statuses.keySet().size());
        assertEquals(7, statuses.get("DRAFT"));
        assertEquals(1, statuses.get("VALID"));
    }
}
