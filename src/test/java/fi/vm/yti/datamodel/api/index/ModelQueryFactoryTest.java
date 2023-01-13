package fi.vm.yti.datamodel.api.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.index.model.ModelSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Collections;
import java.util.Set;

public class ModelQueryFactoryTest {

    ModelQueryFactory factory = new ModelQueryFactory(new ObjectMapper(), new LuceneQueryFactory(new ApplicationProperties()));


    @Test
    public void testModelSearchRequest() throws Exception {
        var expected = EsUtils.getJsonString("/es/modelrequest.json");
        var request = new ModelSearchRequest();
        request.setQuery("test");
        request.setLanguage("fi");
        request.setStatus(Set.of("VALID", "DRAFT"));
        request.setGroups(Set.of("P1"));
        request.setOrganizations(Set.of("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"));
        request.setType(Set.of("profile", "library"));

        SearchRequest searchRequest = factory.createQuery(request, Collections.emptySet());
        JSONAssert.assertEquals(expected, searchRequest.source().toString(), JSONCompareMode.LENIENT);
    }

}
