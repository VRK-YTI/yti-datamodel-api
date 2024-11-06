package fi.vm.yti.datamodel.api.v2.opensearch;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ResourceQueryFactory;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.*;

import static org.springframework.test.util.AssertionErrors.assertEquals;


class ResourceQueryFactoryTest {

    static String modelURI = DataModelURI.createModelURI("test").getModelURI();

    @Test
    void createInternalClassQueryValues() throws Exception {
        var request = new ResourceSearchRequest();
        request.setQuery("test");
        request.setGroups(Set.of("P11", "P1"));
        request.setLimitToDataModel(modelURI);
        request.setFromAddedNamespaces(true);
        request.setPageFrom(1);
        request.setPageSize(100);
        request.setStatus(Set.of(Status.SUGGESTED, Status.VALID));
        request.setSortLang("en");
        request.setTargetClass(modelURI + "TestClass");
        request.setResourceTypes(Set.of(ResourceType.ATTRIBUTE, ResourceType.ASSOCIATION));

        var groupNamespaces = List.of(Constants.DATA_MODEL_NAMESPACE + "groupNs/1.0.0/", Constants.DATA_MODEL_NAMESPACE + "addedNs/1.0.0/");
        var internalNamespaces = List.of(Constants.DATA_MODEL_NAMESPACE + "addedNs/1.0.0/", Constants.DATA_MODEL_NAMESPACE + "draft_model/");
        var externalNamespaces = List.of("http://external-data.com/test");

        var allowedIncompleteDataModels = Set.of(modelURI, Constants.DATA_MODEL_NAMESPACE + "draft_model/");

        var classQuery = ResourceQueryFactory.createInternalResourceQuery(request, externalNamespaces, internalNamespaces,
                groupNamespaces, allowedIncompleteDataModels);

        String expected = MapperTestUtils.getJsonString("/es/classRequest.json");
        JSONAssert.assertEquals(expected, OpenSearchUtils.getPayload(classQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, classQuery.from());
        assertEquals("Page size value not matching", 100, classQuery.size());
    }

    @Test
    void listAttributesInModelWithAdditionalResources() throws Exception {
        var request = new ResourceSearchRequest();

        request.setPageFrom(1);
        request.setPageSize(100);
        request.setLimitToDataModel(modelURI);
        request.setResourceTypes(Set.of(ResourceType.ATTRIBUTE));
        request.setAdditionalResources(Set.of(Constants.DATA_MODEL_NAMESPACE + "ext/some-property"));

        var attributeListQuery = ResourceQueryFactory.createInternalResourceQuery(request, new ArrayList<>(), new ArrayList<>(),
                null, new HashSet<>());
        String expected = MapperTestUtils.getJsonString("/es/attributeListRequest.json");
        JSONAssert.assertEquals(expected, OpenSearchUtils.getPayload(attributeListQuery), JSONCompareMode.LENIENT);
    }

    @Test
    void listClassesByVersion() throws Exception {
        var request = new ResourceSearchRequest();

        request.setPageFrom(1);
        request.setPageSize(100);
        request.setLimitToDataModel(modelURI);
        request.setResourceTypes(Set.of(ResourceType.CLASS));
        request.setFromVersion("1.2.3");

        var query = ResourceQueryFactory.createInternalResourceQuery(request, new ArrayList<>(), new ArrayList<>(),
                null, new HashSet<>());
        String expected = MapperTestUtils.getJsonString("/es/classesByVersion.json");
        JSONAssert.assertEquals(expected, OpenSearchUtils.getPayload(query), JSONCompareMode.LENIENT);
    }

    @Test
    void createInternalClassQueryDefaults() {
        var request = new ResourceSearchRequest();

        var classQuery = ResourceQueryFactory.createInternalResourceQuery(request, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptySet());

        assertEquals("Page from value not matching", QueryFactoryUtils.DEFAULT_PAGE_FROM, classQuery.from());
        assertEquals("Page size value not matching", QueryFactoryUtils.DEFAULT_PAGE_SIZE, classQuery.size());
        assertEquals("Label should be sorted in finnish by default", "label.fi.sortKey", classQuery.sort().get(0).field().field());
    }

    @Test
    void testPagination() {
        var request1 = getPaginationQuery(null, null);
        assertEquals("Incorrect page start from", 0, request1.from());
        assertEquals("Incorrect page size", 10, request1.size());

        var request2 = getPaginationQuery(1, 20);
        assertEquals("Incorrect page start from", 0, request2.from());
        assertEquals("Incorrect page size", 20, request2.size());

        var request3 = getPaginationQuery(3, null);
        assertEquals("Incorrect page start from", 20, request3.from());
        assertEquals("Incorrect page size", 10, request3.size());
    }

    private SearchRequest getPaginationQuery(Integer page, Integer size) {
        var request = new ResourceSearchRequest();
        request.setLimitToDataModel(modelURI);
        request.setPageFrom(page);
        request.setPageSize(size);

        return ResourceQueryFactory.createInternalResourceQuery(request,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new HashSet<>());
    }
}
