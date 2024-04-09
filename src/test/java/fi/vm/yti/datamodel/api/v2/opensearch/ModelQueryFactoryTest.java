package fi.vm.yti.datamodel.api.v2.opensearch;

import fi.vm.yti.datamodel.api.index.OpenSearchUtils;
import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.util.AssertionErrors.assertEquals;

class ModelQueryFactoryTest {

    @Test
    void createModelClassQueryTest() throws Exception {
        var request = new ModelSearchRequest();
        request.setQuery("test query");
        request.setGroups(Set.of("P11", "P1"));
        request.setType(Set.of(ModelType.PROFILE));
        request.setSearchResources(true);
        request.setOrganizations(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));
        request.setLanguage("en");
        request.setPageFrom(1);
        request.setPageSize(100);
        request.setStatus(Set.of(Status.SUGGESTED, Status.VALID));
        request.setSortLang("en");
        request.setIncludeDraftFrom(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));

        var modelQuery = ModelQueryFactory.createModelQuery(request, false);

        String expected = MapperTestUtils.getJsonString("/es/modelrequest.json");
        JSONAssert.assertEquals(expected, OpenSearchUtils.getPayload(modelQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 1, modelQuery.from());
        assertEquals("Page size value not matching", 100, modelQuery.size());
    }

    @Test
    void createModelClassQueryDefaultsTest() {
        var request = new ModelSearchRequest();

        var modelQuery = ModelQueryFactory.createModelQuery(request, false);

        assertEquals("Page from value not matching", 0, modelQuery.from());
        assertEquals("Page size value not matching", 10, modelQuery.size());
        assertEquals("Label should be sorted in finnish by default", "label.fi.keyword", modelQuery.sort().get(0).field().field());

    }


}
