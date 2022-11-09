package fi.vm.yti.datamodel.api.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.index.model.ModelSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.Test;
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

        SearchRequest searchRequest = factory.createQuery(request, Collections.emptySet());
        JSONAssert.assertEquals(expected, searchRequest.source().toString(), JSONCompareMode.LENIENT);
    }

}
