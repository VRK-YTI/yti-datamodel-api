package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ExternalNamespaceDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelVersionInfo;
import fi.vm.yti.datamodel.api.v2.dto.VersionedModelDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        DataModelService.class
})
class DataModelServiceTest {

    @MockBean
    CoreRepository coreRepository;

    @MockBean
    DataModelAuthorizationManager authorizationManager;

    @MockBean
    GroupManagementService groupManagementService;

    @MockBean
    ModelMapper modelMapper;

    @MockBean
    TerminologyService terminologyService;

    @MockBean
    VisualizationService visualizationService;

    @MockBean
    CodeListService codeListService;

    @MockBean
    IndexService indexService;

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    DataModelSubscriptionService dataModelSubscriptionService;

    @Autowired
    DataModelService dataModelService;

    private static final UUID RANDOM_ORG = UUID.randomUUID();

    @Test
    void getWithoutVersion() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        assertThrows(ResourceNotFoundException.class, () -> dataModelService.get("test", null));
    }

    @Test
    void getLatest() {
        var versionIri = "https://iri.suomi.fi/model/test/1.0.0/";

        var solution = mock(QuerySolution.class);
        var rdfNode = mock(RDFNode.class);
        when(rdfNode.toString()).thenReturn(versionIri);
        when(solution.get("version")).thenReturn(rdfNode);

        doAnswer(a -> {
            var arg = a.getArgument(1, Consumer.class);
            arg.accept(solution);
            return null;
        }).when(coreRepository).querySelect(any(Query.class), any(Consumer.class));

        dataModelService.get("test", null);

        verify(coreRepository).fetch(versionIri);
    }

    @Test
    void getWithVersion() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.get("test", "1.0.1");

        verify(coreRepository).fetch(Constants.DATA_MODEL_NAMESPACE + "test/1.0.1" + Constants.RESOURCE_SEPARATOR);
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(modelMapper).mapToDataModelDTO(anyString(), any(Model.class), eq(null));
    }

    @Test
    void create() throws URISyntaxException {
        when(authorizationManager.hasRightToAnyOrganization(anyCollection(), eq(Role.DATA_MODEL_EDITOR))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        when(modelMapper.mapToJenaModel(any(DataModelDTO.class), any(GraphType.class), any(YtiUser.class))).thenReturn(ModelFactory.createDefaultModel());
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(new IndexModel());
        var dto = createDatamodelDTO(false);
        var uri = dataModelService.create(dto, GraphType.LIBRARY);
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/", uri.toString());

        verify(authorizationManager).hasRightToAnyOrganization(anyCollection(), eq(Role.DATA_MODEL_EDITOR));
        verify(terminologyService).resolveTerminology(anySet());
        verify(codeListService).resolveCodelistScheme(anySet());
        verify(modelMapper).mapToJenaModel(any(DataModelDTO.class), any(GraphType.class), any(YtiUser.class));
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(modelMapper).mapToIndexModel(anyString(), any(Model.class));
        verify(indexService).createModelToIndex(any(IndexModel.class));
    }

    @Test
    void update() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(new IndexModel());
        var dto = createDatamodelDTO(true);
        dataModelService.update("test", dto);


        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveTerminology(anySet());
        verify(codeListService).resolveCodelistScheme(anySet());
        verify(modelMapper).mapToUpdateJenaModel(anyString(), any(DataModelDTO.class), any(Model.class), any(YtiUser.class));
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(modelMapper).mapToIndexModel(anyString(), any(Model.class));
        verify(indexService).updateModelToIndex(any(IndexModel.class));
    }

    @Test
    void deleteValidModel() {
        var dataModelURI = DataModelURI.createModelURI("test", "1.0.0");

        var model = ModelFactory.createDefaultModel();
        model.createResource(dataModelURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.VALID));

        when(coreRepository.graphExists(dataModelURI.getGraphURI())).thenReturn(true);
        when(coreRepository.fetch(dataModelURI.getGraphURI())).thenReturn(model);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockAdminUser);
        when(authorizationManager.hasAdminRightToModel(eq(dataModelURI.getModelId()), any(Model.class))).thenReturn(true);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(ModelFactory.createDefaultModel());

        var error = assertThrows(MappingError.class, () -> dataModelService.delete("test", "1.0.0"));

        assertTrue(error.getMessage().contains("Cannot remove data model with status VALID"),
                "Unexpected error message: " + error.getMessage());

        verify(coreRepository, never()).delete(anyString());
        verify(indexService, never()).deleteModelFromIndex(anyString());
        verify(indexService, never()).removeResourceIndexesByDataModel(anyString(), anyString());
    }

    @ParameterizedTest
    @CsvSource({ "RETIRED", "SUPERSEDED", "SUGGESTED", "DRAFT" })
    void deleteVersionedModelWithReferrers(String status) {
        var dataModelURI = DataModelURI.createModelURI("test", "1.0.0");

        var model = ModelFactory.createDefaultModel();
        model.createResource(dataModelURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.valueOf(status)));

        when(userProvider.getUser()).thenReturn(EndpointUtils.mockAdminUser);
        when(authorizationManager.hasAdminRightToModel(eq(dataModelURI.getModelId()), any(Model.class))).thenReturn(true);
        when(coreRepository.graphExists(dataModelURI.getGraphURI())).thenReturn(true);
        when(coreRepository.fetch(dataModelURI.getGraphURI())).thenReturn(model);

        var refCheckResult = ModelFactory.createDefaultModel();
        refCheckResult.createResource("https://iri.suomi.fi/model/some-ref-model/")
                .addProperty(OWL.imports, ResourceFactory.createResource(dataModelURI.getGraphURI()));

        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(refCheckResult);

        // draft and suggested can be removed even if there are referrers
        if (List.of("DRAFT", "SUGGESTED").contains(status)) {
            dataModelService.delete("test", "1.0.0");
            verify(coreRepository).delete(dataModelURI.getGraphURI());
            verify(indexService).deleteModelFromIndex(dataModelURI.getGraphURI());
            verify(indexService).removeResourceIndexesByDataModel(dataModelURI.getModelURI(), "1.0.0");
        } else {
            var error = assertThrows(MappingError.class, () -> dataModelService.delete("test", "1.0.0"));
            assertTrue(error.getMessage().contains("Cannot remove data model with references"),
                    "Unexpected error message: " + error.getMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({ "RETIRED", "SUPERSEDED", "SUGGESTED", "DRAFT" })
    void deleteVersionedModelWithoutReferrers(String status) {
        var dataModelURI = DataModelURI.createModelURI("test", "1.0.0");

        var model = ModelFactory.createDefaultModel();
        model.createResource(dataModelURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.valueOf(status)));

        when(userProvider.getUser()).thenReturn(EndpointUtils.mockAdminUser);
        when(authorizationManager.hasAdminRightToModel(eq(dataModelURI.getModelId()), any(Model.class))).thenReturn(true);
        when(coreRepository.graphExists(dataModelURI.getGraphURI())).thenReturn(true);
        when(coreRepository.fetch(dataModelURI.getGraphURI())).thenReturn(model);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.delete("test", "1.0.0");
        verify(coreRepository).delete(dataModelURI.getGraphURI());
        verify(indexService).deleteModelFromIndex(dataModelURI.getGraphURI());
        verify(indexService).removeResourceIndexesByDataModel(dataModelURI.getModelURI(), "1.0.0");
    }

    @Test
    void deleteUnauthorized() {
        when(coreRepository.graphExists(anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasAdminRightToModel(anyString(), any(Model.class))).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> dataModelService.delete("test", null));

        verify(coreRepository, never()).delete(anyString());
        verifyNoInteractions(indexService);
    }

    @Test
    void deleteNotExists() {
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockSuperUser);

        assertThrows(ResourceNotFoundException.class, () -> dataModelService.delete("test", null));

        verify(coreRepository, never()).delete(anyString());
        verifyNoInteractions(indexService);
    }

    @Test
    void exists() {
        var response = dataModelService.exists("test");
        assertFalse(response);

        //reserved
        response = dataModelService.exists("http");
        assertTrue(response);

        //exists
        when(coreRepository.graphExists(anyString())).thenReturn(true);
        response = dataModelService.exists("test");
        assertTrue(response);
    }

    @Test
    void shouldExport() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);

        var response = dataModelService.export("test", null, "text/turtle", false, "fi");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test:TestClass"));
        assertTrue(response.getBody().contains("test:TestAttribute"));
        assertTrue(response.getBody().contains("test:TestAssociation"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/ld+json", "text/turtle", "application/rdf+xml"})
    void shouldGetModelWithAcceptHeader(String accept) {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        var response = dataModelService.export("test", null, accept, false, "fi");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        //TODO can this be fixed
    }

    @Test
    void shouldRemoveTriplesHiddenFromUnauthenticatedUser() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(false);
        var response = dataModelService.export("test", null, "text/turtle", false, "fi");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().contains("skos:editorialNote"));
    }


    @Test
    void testCreateRelease() throws URISyntaxException {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        //remove prior version property since we want to test without
        model.getResource(Constants.DATA_MODEL_NAMESPACE + "test").removeAll(OWL.priorVersion);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(mock(IndexModel.class));
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);

        dataModelService.createRelease("test", "1.0.1", Status.VALID);

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(modelMapper).mapReleaseProperties(any(Model.class),
                any(DataModelURI.class),
                eq(Status.VALID));
        verify(modelMapper).mapPriorVersion(any(Model.class),
                                            eq(Constants.DATA_MODEL_NAMESPACE + "test/"),
                                            eq(Constants.DATA_MODEL_NAMESPACE + "test/1.0.1/"));

        verify(coreRepository).put(eq(Constants.DATA_MODEL_NAMESPACE + "test/1.0.1/"), any(Model.class));
        verify(coreRepository).put(eq(Constants.DATA_MODEL_NAMESPACE + "test/"), any(Model.class));

        verify(modelMapper).mapToIndexModel(eq(Constants.DATA_MODEL_NAMESPACE + "test/"), any(Model.class));
        verify(indexService).createModelToIndex(any(IndexModel.class));
        verify(visualizationService).saveVersionedPositions("test", "1.0.1");

        var captor = ArgumentCaptor.forClass(String.class);
        verify(dataModelSubscriptionService).publish(eq("test"), anyString(), captor.capture());
        assertTrue(captor.getValue().contains("version 1.0.1"), "version not included to the message");
        assertTrue(captor.getValue().contains("testlabel"), "label not included to the message");
    }

    @Test
    void testCreateReleaseInvalidVersions(){
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);

        var error = assertThrows(MappingError.class ,() -> dataModelService.createRelease("test", "not valid semver", Status.VALID));
        assertEquals("Error during mapping: Not valid Semantic version string", error.getMessage());

        error = assertThrows(MappingError.class ,() -> dataModelService.createRelease("test", "v1.2.3", Status.VALID));
        assertEquals("Error during mapping: Not valid Semantic version string", error.getMessage());

        error = assertThrows(MappingError.class ,() -> dataModelService.createRelease("test", "1.0.0", Status.VALID));
        assertEquals("Error during mapping: Same version number", error.getMessage());

        error = assertThrows(MappingError.class ,() -> dataModelService.createRelease("test", "0.1.0", Status.VALID));
        assertEquals("Error during mapping: Older version given", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"VALID", "SUGGESTED"}, mode = EnumSource.Mode.EXCLUDE)
    void testCreateReleaseInvalidStatus(Status status){
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);

        var error = assertThrows(MappingError.class ,() -> dataModelService.createRelease("test", "1.0.1", status));
        assertEquals("Error during mapping: Status has to be SUGGESTED or VALID", error.getMessage());
    }

    @Test
    void testGetPriorVersions(){
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_prior_versions.ttl");
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(model);
        when(modelMapper.mapModelVersionInfo(any(Resource.class))).thenReturn(mock(ModelVersionInfo.class));
        var result = dataModelService.getPriorVersions("test", null);

        verify(coreRepository).queryConstruct(any(Query.class));
        verify(modelMapper, times(2)).mapModelVersionInfo(any(Resource.class));
        assertEquals(2, result.size());
    }

    @Test
    void testGetPriorVersionsWithVersion(){
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_prior_versions.ttl");
        var mockVersionInfo = new ModelVersionInfo();
        mockVersionInfo.setVersion("1.0.2");
        mockVersionInfo.setStatus(Status.VALID);
        mockVersionInfo.setVersionIRI(Constants.DATA_MODEL_NAMESPACE + "test/1.0.2");
        when(modelMapper.mapModelVersionInfo(any(Resource.class))).thenReturn(mockVersionInfo);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(model);
        var result = dataModelService.getPriorVersions("test", "1.0.1");

        verify(coreRepository).queryConstruct(any(Query.class));
        verify(modelMapper, times(2)).mapModelVersionInfo(any(Resource.class));
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateVersionedDatamodel() {
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(new IndexModel());


        dataModelService.updateVersionedModel("test", "1.0.1", new VersionedModelDTO());

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(modelMapper).mapUpdateVersionedModel(any(Model.class), anyString(), any(VersionedModelDTO.class), any(YtiUser.class));
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(modelMapper).mapToIndexModel(anyString(), any(Model.class));
        verify(indexService).updateModelToIndex(any(IndexModel.class));
    }

    @Test
    void changeLinkedModelVersions() {
        var resourceURI = DataModelURI.createResourceURI("test", "class-1");
        var linkedResourceURI_1 = DataModelURI.createResourceURI("linked", "link-1", "1.0.0");
        var linkedResourceURI_2 = DataModelURI.createResourceURI("linked", "link-1", "2.0.0");
        var linkedResourceURI_3 = DataModelURI.createResourceURI("foo", "bar-1", "1.0.0");
        var model = ModelFactory.createDefaultModel();
        var prefix = resourceURI.getModelId();

        // create model with one class with some references to other models' classes
        model.createResource(resourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(OWL.imports, ResourceFactory.createResource(linkedResourceURI_1.getGraphURI()));

        model.createResource(resourceURI.getResourceURI())
                .addProperty(RDFS.subClassOf, ResourceFactory.createResource(linkedResourceURI_1.getResourceVersionURI()))
                .addProperty(OWL.disjointWith, ResourceFactory.createResource(linkedResourceURI_3.getResourceVersionURI()));

        when(coreRepository.fetch(resourceURI.getGraphURI())).thenReturn(model);
        when(authorizationManager.hasRightToModel(eq(prefix), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);

        var dto = new DataModelDTO();
        var resourceSubject = ResourceFactory.createResource(resourceURI.getResourceURI());
        var modelSubject = ResourceFactory.createResource(resourceURI.getModelURI());

        // should change references to version 2.0.0
        dto.setInternalNamespaces(Set.of(linkedResourceURI_2.getGraphURI()));

        dataModelService.update(prefix, dto);

        // rdfs:subClassOf -> version 2.0.0
        assertTrue(model.contains(
                resourceSubject,
                RDFS.subClassOf,
                ResourceFactory.createResource(linkedResourceURI_2.getResourceVersionURI())));

        // references to other models should not change
        assertTrue(model.contains(
                resourceSubject,
                OWL.disjointWith,
                ResourceFactory.createResource(linkedResourceURI_3.getResourceVersionURI())));

        // owl:imports -> version 2.0.0
        assertTrue(model.contains(
                modelSubject,
                OWL.imports,
                ResourceFactory.createResource(linkedResourceURI_2.getGraphURI())));

        // model doesn't contain any references to version 1.0.0
        assertTrue(model.listObjects().toList().stream()
                .noneMatch(containsReferences(linkedResourceURI_1.getGraphURI())));

        // change reference to draft version
        dto.setInternalNamespaces(Set.of(linkedResourceURI_2.getDraftGraphURI()));
        dataModelService.update(prefix, dto);

        // rdfs:subClassOf -> draft version (without version number)
        assertTrue(model.contains(
                resourceSubject,
                RDFS.subClassOf,
                ResourceFactory.createResource(linkedResourceURI_2.getResourceURI())));

        assertTrue(model.contains(
                resourceSubject,
                OWL.disjointWith,
                ResourceFactory.createResource(linkedResourceURI_3.getResourceVersionURI())));

        assertTrue(model.contains(
                modelSubject,
                OWL.imports,
                ResourceFactory.createResource(linkedResourceURI_2.getDraftGraphURI())));

        // model doesn't contain any references to version 2.0.0
        assertTrue(model.listObjects().toList().stream()
                .noneMatch(containsReferences(linkedResourceURI_2.getGraphURI())));

        // change back to version 1.0.0
        dto.setInternalNamespaces(Set.of(linkedResourceURI_1.getGraphURI()));
        dataModelService.update(prefix, dto);

        assertTrue(model.contains(
                resourceSubject,
                RDFS.subClassOf,
                ResourceFactory.createResource(linkedResourceURI_1.getResourceVersionURI())));

        assertTrue(model.contains(
                resourceSubject,
                OWL.disjointWith,
                ResourceFactory.createResource(linkedResourceURI_3.getResourceVersionURI())));

        assertTrue(model.contains(
                modelSubject,
                OWL.imports,
                ResourceFactory.createResource(linkedResourceURI_1.getGraphURI())));

        // model doesn't contain any references to draft version
        assertTrue(model.listObjects().toList().stream()
                .noneMatch(containsReferences(linkedResourceURI_1.getDraftGraphURI())));
    }

    @Test
    void testRemoveLinkedNamespace() {
        var resourceURI = DataModelURI.createResourceURI("test", "class-1");
        var linkedResourceURI_1 = DataModelURI.createResourceURI("linked", "link-1", "1.0.0");
        var linkedNsWithoutReferences = DataModelURI.createModelURI("foo", "1.0.0").getGraphURI();
        var model = ModelFactory.createDefaultModel();
        var prefix = resourceURI.getModelId();

        model.createResource(resourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(OWL.imports, ResourceFactory.createResource(linkedResourceURI_1.getGraphURI()))
                .addProperty(OWL.imports, ResourceFactory.createResource(linkedNsWithoutReferences));

        model.createResource(resourceURI.getResourceURI())
                .addProperty(RDFS.subClassOf, ResourceFactory.createResource(linkedResourceURI_1.getResourceVersionURI()));

        when(coreRepository.fetch(resourceURI.getGraphURI())).thenReturn(model);
        when(authorizationManager.hasRightToModel(eq(prefix), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);

        var dto = new DataModelDTO();
        dto.setInternalNamespaces(Set.of(linkedNsWithoutReferences));

        var error = assertThrows(MappingError.class, () -> dataModelService.update(prefix, dto));

        // error message should contain linked resources
        assertTrue(error.getMessage().contains(resourceURI.getResourceURI()));

        // linked model without any references should get removed
        dto.setInternalNamespaces(Set.of(linkedResourceURI_1.getGraphURI()));
        dataModelService.update(prefix, dto);

        verify(coreRepository).put(anyString(), any(Model.class));
    }

    @Test
    void testCopyGraph() {
        var oldGraphURI = DataModelURI.createModelURI("test", "1.0.0");
        var newGraphURI = DataModelURI.createModelURI("new_prefix");
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var mockUser = EndpointUtils.mockUser;

        when(userProvider.getUser()).thenReturn(mockUser);
        when(coreRepository.fetch(oldGraphURI.getGraphURI())).thenReturn(model);
        when(authorizationManager.hasRightToModel(eq(oldGraphURI.getModelId()), any(Model.class))).thenReturn(true);
        when(coreRepository.graphExists(oldGraphURI.getGraphURI())).thenReturn(true);
        when(coreRepository.graphExists(newGraphURI.getGraphURI())).thenReturn(false);
        when(modelMapper.mapToIndexModel(eq(newGraphURI.getModelURI()), any(Model.class))).thenReturn(new IndexModel());

        dataModelService.copyDataModel(oldGraphURI.getModelId(), "1.0.0", newGraphURI.getModelId());

        var captor = ArgumentCaptor.forClass(Model.class);

        verify(coreRepository).put(eq(newGraphURI.getGraphURI()), captor.capture());
        verify(visualizationService).copyVisualization("test", "1.0.0", "new_prefix");
        verify(indexService).createModelToIndex(any(IndexModel.class));
        verify(indexService).indexGraphResource(any(Model.class));

        var copy = captor.getValue();
        var modelResource = copy.getResource(newGraphURI.getModelURI());

        // Subject count should match in original and copied
        assertEquals(model.listSubjects().toList().size(), copy.listSubjects().toList().size());

        // All version related triples should be removed
        assertFalse(modelResource.hasProperty(OWL.versionInfo));
        assertFalse(modelResource.hasProperty(OWL2.versionIRI));
    }

    @Test
    void testCreateNewDraft() {
        // create new draft from version 1.0.0
        var graphURI = DataModelURI.createModelURI("test", "1.0.0");

        var draft = ModelFactory.createDefaultModel();
        var version = ModelFactory.createDefaultModel();

        draft.createResource(graphURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));

        when(userProvider.getUser()).thenReturn(EndpointUtils.mockAdminUser);
        when(coreRepository.fetch(graphURI.getGraphURI())).thenReturn(version);
        when(coreRepository.graphExists(graphURI.getDraftGraphURI())).thenReturn(false);
        when(authorizationManager.hasAdminRightToModel(eq(graphURI.getModelId()), any(Model.class))).thenReturn(true);
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(new IndexModel());
        when(modelMapper.mapNewDraft(any(Model.class), any(DataModelURI.class))).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.createDraft(graphURI.getModelId(), graphURI.getVersion());

        // new draft should be saved
        verify(coreRepository).put(eq(graphURI.getDraftGraphURI()), any(Model.class));
        // add new draft to index
        verify(indexService).createModelToIndex(any(IndexModel.class));
        verify(indexService).indexGraphResource(any(Model.class));
    }

    @Test
    void testCreateNewDraftExisting() {
        var graphURI = DataModelURI.createModelURI("test", "1.0.0");

        when(coreRepository.graphExists(graphURI.getDraftGraphURI())).thenReturn(true);

        var error = assertThrows(MappingError.class,
                () -> dataModelService.createDraft("test", "1.0.0"));

        assertTrue(error.getMessage().contains("Draft graph exists"),
                "Unexpected error creating new draft with existing one.");

        // new draft should be saved
        verify(coreRepository, never()).put(anyString(), any(Model.class));
        // add new draft to index
        verify(indexService, never()).createModelToIndex(any(IndexModel.class));
        verify(indexService, never()).indexGraphResource(any(Model.class));

    }

    private Predicate<RDFNode> containsReferences(String ns) {
        return (var o) -> o.isResource() && o.asResource().getNameSpace().equals(ns);
    }

    /**
     * Create Datamodel DTO for testing
     * @param updateModel true if model will be used for updating instead of creating
     * @return DataModelDTO
     */
    private static DataModelDTO createDatamodelDTO(boolean updateModel){
        DataModelDTO dataModelDTO = new DataModelDTO();
        dataModelDTO.setDescription(Map.of("fi", "test description"));
        dataModelDTO.setLabel(Map.of("fi", "test label"));
        dataModelDTO.setGroups(Set.of("P11"));
        dataModelDTO.setLanguages(Set.of("fi"));
        dataModelDTO.setOrganizations(Set.of(RANDOM_ORG));
        dataModelDTO.setInternalNamespaces(Set.of(Constants.DATA_MODEL_NAMESPACE + "test"));
        var extNs = new ExternalNamespaceDTO();
        extNs.setName(Map.of("fi", "test external namespace"));
        extNs.setPrefix("testprefix");
        extNs.setNamespace("http://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(extNs));
        if(!updateModel){
            dataModelDTO.setPrefix("test");
        }
        dataModelDTO.setTerminologies(Set.of("http://uri.suomi.fi/terminology/test"));
        return dataModelDTO;
    }


}
