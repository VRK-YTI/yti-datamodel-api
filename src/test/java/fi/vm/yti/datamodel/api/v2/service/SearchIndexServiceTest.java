package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ModelSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ModelQueryFactory;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.ResourceQueryFactory;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        SearchIndexService.class
})
class SearchIndexServiceTest {

    @MockBean
    private OpenSearchClientWrapper client;
    @MockBean
    private GroupManagementService groupManagementService;
    @MockBean
    private TerminologyService terminologyService;
    @MockBean
    private DataModelService dataModelService;
    @MockBean
    private ResourceService resourceService;
    @MockBean
    private CoreRepository coreRepository;

    @Captor
    private ArgumentCaptor<ResourceSearchRequest> resourceCaptor;
    @Captor
    private ArgumentCaptor<ModelSearchRequest> modelCaptor;
    @Captor
    private ArgumentCaptor<List<String>> externalNamespaceCaptor;
    @Captor
    private ArgumentCaptor<List<String>> internalNamespaceCaptor;
    @Captor
    private ArgumentCaptor<List<String>> restrictedDataModelsCaptor;
    @Captor
    private ArgumentCaptor<Set<String>> allowedDataModelsCaptor;

    @Autowired
    SearchIndexService searchIndexService;

    private static final String DATA_MODEL_URI = Constants.DATA_MODEL_NAMESPACE + "profile" + Constants.RESOURCE_SEPARATOR;

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Test
    void allowedDataModelsForNonSuperUsers() {
        var request = new ResourceSearchRequest();
        request.setResourceTypes(Set.of(ResourceType.CLASS));

        doSearch(request);

        assertEquals(ORGANIZATION_ID, modelCaptor.getValue().getIncludeDraftFrom().iterator().next());
        assertEquals("1", allowedDataModelsCaptor.getValue().iterator().next());
    }

    @Test
    void getClassList() {
        var request = new ResourceSearchRequest();
        request.setLimitToDataModel(DATA_MODEL_URI);
        request.setResourceTypes(Set.of(ResourceType.CLASS));

        doSearch(request);

        assertNull(resourceCaptor.getValue().getAdditionalResources());
    }

    @Test
    void getAttributeList() {
        var request = new ResourceSearchRequest();
        request.setLimitToDataModel(DATA_MODEL_URI);
        request.setResourceTypes(Set.of(ResourceType.ATTRIBUTE));

        doSearch(request);

        assertEquals("ext-1", resourceCaptor.getValue().getAdditionalResources().iterator().next());
    }

    @Test
    void findResourcesFromNamespaces() {
        var request = new ResourceSearchRequest();
        request.setLimitToDataModel(DATA_MODEL_URI);
        request.setFromAddedNamespaces(true);
        request.setResourceTypes(Set.of(ResourceType.CLASS));

        doSearch(request);

        assertEquals(Constants.DATA_MODEL_NAMESPACE + "model", internalNamespaceCaptor.getValue().iterator().next());
        assertEquals("http://ext.org/datamodel", externalNamespaceCaptor.getValue().iterator().next());
    }

    @Test
    void findWithDataModelRestrictions() {
        var request = new ResourceSearchRequest();
        request.setGroups(Set.of("group-1"));
        request.setLimitToModelType(GraphType.LIBRARY);
        request.setStatus(Set.of(Status.VALID));
        request.setResourceTypes(Set.of(ResourceType.CLASS));
        request.setLimitToDataModel(DATA_MODEL_URI);
        request.setFromAddedNamespaces(true);

        doSearch(request);

        var modelRestrictionSearch = modelCaptor.getAllValues().get(1);

        assertEquals("group-1", modelRestrictionSearch.getGroups().iterator().next());
        assertEquals(Status.VALID, modelRestrictionSearch.getStatus().iterator().next());
        assertEquals(GraphType.LIBRARY, modelRestrictionSearch.getType().iterator().next());
        assertEquals(1, externalNamespaceCaptor.getValue().size());
        assertEquals(1, internalNamespaceCaptor.getValue().size());
        assertEquals("2", restrictedDataModelsCaptor.getValue().iterator().next());
        assertEquals("1", allowedDataModelsCaptor.getValue().iterator().next());
    }

    private void doSearch(ResourceSearchRequest request) {
        // user's organizations
        when(groupManagementService.getOrganizationsForUser(any(YtiUser.class))).thenReturn(Set.of(ORGANIZATION_ID));

        // external property for attribute list
        when(resourceService.findNodeShapeExternalProperties(DATA_MODEL_URI, OWL.DatatypeProperty))
                .thenReturn(Set.of("ext-1"));

        // model's namespaces
        var model = ModelFactory.createDefaultModel();
        model.createResource(DATA_MODEL_URI)
                .addProperty(OWL.imports, ResourceFactory.createResource(Constants.DATA_MODEL_NAMESPACE + "model"))
                .addProperty(OWL.imports, ResourceFactory.createResource("http://ext.org/datamodel"))
                .addProperty(DCTerms.requires, ResourceFactory.createResource("http://uri.suomi.fi/terminology/test-123"));
        when(coreRepository.fetch(DATA_MODEL_URI)).thenReturn(model);

        var allowedModel = new IndexModel();
        allowedModel.setId("1");
        var allowedModelResponse = new SearchResponseDTO<IndexModel>();
        allowedModelResponse.setResponseObjects(List.of(allowedModel));

        var restrictedModel = new IndexModel();
        restrictedModel.setId("2");
        var restrictedModelResponse = new SearchResponseDTO<IndexModel>();
        restrictedModelResponse.setResponseObjects(List.of(restrictedModel));

        when(client.search(argThat(a -> a != null && a.query() != null && a.query().bool().must().isEmpty()),
                eq(IndexModel.class))).thenReturn(allowedModelResponse);

        when(client.search(argThat(a -> a != null && a.query() != null && a.query().bool().must().size() == 1),
                eq(IndexModel.class))).thenReturn(restrictedModelResponse);

        try (var resourceQueryFactory = mockStatic(ResourceQueryFactory.class);
             var modelQueryFactory = mockStatic(ModelQueryFactory.class)) {

            var emptyQuery = QueryBuilders.bool().must(List.of()).build().toQuery();

            // resource query
            resourceQueryFactory.when(() -> ResourceQueryFactory.createInternalResourceQuery(
                            any(ResourceSearchRequest.class),
                            anyList(),
                            anyList(),
                            eq(null),
                            anySet()))
                    .thenReturn(new SearchRequest.Builder().query(emptyQuery).build());

            // get allowed data models
            modelQueryFactory.when(() -> ModelQueryFactory.createModelQuery(
                            argThat(r -> r.getGroups() == null), eq(false)))
                    .thenReturn(new SearchRequest.Builder().query(emptyQuery).build());

            // models by search criteria
            var modelQuery = QueryBuilders.bool()
                    .must(QueryFactoryUtils.termsQuery("isPartOf", Set.of("group-1")))
                    .build().toQuery();

            modelQueryFactory.when(() -> ModelQueryFactory.createModelQuery(
                            argThat(r -> r.getGroups() != null), eq(false)))
                    .thenReturn(new SearchRequest.Builder().query(modelQuery).build());

            searchIndexService.searchInternalResources(request, EndpointUtils.mockUser);

            var times = request.getGroups() == null ? 1 : 2;
            modelQueryFactory.verify(() -> ModelQueryFactory.createModelQuery(modelCaptor.capture(), eq(false)),
                    times(times));
            resourceQueryFactory.verify(() -> ResourceQueryFactory.createInternalResourceQuery(
                    resourceCaptor.capture(),
                    externalNamespaceCaptor.capture(),
                    internalNamespaceCaptor.capture(),
                    restrictedDataModelsCaptor.capture(),
                    allowedDataModelsCaptor.capture()));
        }
    }
}
