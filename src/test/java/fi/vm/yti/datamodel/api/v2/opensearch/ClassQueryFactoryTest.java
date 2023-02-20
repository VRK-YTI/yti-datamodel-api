package fi.vm.yti.datamodel.api.v2.opensearch;

import fi.vm.yti.datamodel.api.index.OpenSearchUtils;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ClassSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ClassQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.QueryFactoryUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Collections;
import java.util.Set;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNull;


class ClassQueryFactoryTest {




    @Test
    void createInternalClassQueryValues() throws Exception {
        var request = new ClassSearchRequest();
        request.setQuery("test query");
        request.setGroups(Set.of("P11", "P1"));
        request.setFromAddedNamespaces("http://uri.suomi.fi/datamodel/ns/test");
        request.setPageFrom(1);
        request.setPageSize(100);
        request.setStatus(Set.of(Status.DRAFT, Status.VALID));
        request.setSortLang("en");

        var groupNamespaces = Set.of("http://uri.suomi.fi/datamodel/ns/groupNs");
        var addedNamespaces = Set.of("http://uri.suomi.fi/datamodel/ns/addedNs");

        var classQuery = ClassQueryFactory.createInternalClassQuery(request, addedNamespaces, groupNamespaces);

        String expected = OpenSearchUtils.getJsonString("/es/classRequest.json");
        JSONAssert.assertEquals(expected, OpenSearchUtils.getPayload(classQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 1, classQuery.from());
        assertEquals("Page size value not matching", 100, classQuery.size());
    }

    @Test
    void createInternalClassQueryDefaults() {
        var request = new ClassSearchRequest();

        var classQuery = ClassQueryFactory.createInternalClassQuery(request, Collections.emptySet(), Collections.emptySet());

        assertEquals("Page from value not matching", QueryFactoryUtils.DEFAULT_PAGE_FROM, classQuery.from());
        assertEquals("Page size value not matching", QueryFactoryUtils.DEFAULT_PAGE_SIZE, classQuery.size());
        assertEquals("Label should be sorted in finnish by default", "label.fi.keyword", classQuery.sort().get(0).field().field());
        assertNull("No query should be by default", classQuery.query());
    }
}
