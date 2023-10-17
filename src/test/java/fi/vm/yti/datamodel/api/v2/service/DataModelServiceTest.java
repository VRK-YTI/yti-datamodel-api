package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        DataModelService.class
})
class DataModelServiceTest {

    @MockBean
    CoreRepository coreRepository;

    @MockBean
    AuthorizationManager authorizationManager;

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
    OpenSearchIndexer openSearchIndexer;

    @MockBean
    AuthenticatedUserProvider userProvider;

    @Autowired
    DataModelService dataModelService;

    private static final UUID RANDOM_ORG = UUID.randomUUID();

    @Test
    void get() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.get("test", null);

        verify(coreRepository).fetch("http://uri.suomi.fi/datamodel/ns/test");
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(modelMapper).mapToDataModelDTO(anyString(), any(Model.class), eq(null));
    }

    @Test
    void getWithVersion() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.get("test", "1.0.1");

        verify(coreRepository).fetch("http://uri.suomi.fi/datamodel/ns/test/1.0.1");
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(modelMapper).mapToDataModelDTO(anyString(), any(Model.class), eq(null));
    }

    @Test
    void create() throws URISyntaxException {
        when(authorizationManager.hasRightToAnyOrganization(anyCollection())).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        when(modelMapper.mapToJenaModel(any(DataModelDTO.class), any(ModelType.class), any(YtiUser.class))).thenReturn(ModelFactory.createDefaultModel());
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(new IndexModel());
        var dto = createDatamodelDTO(false);
        var uri = dataModelService.create(dto, ModelType.LIBRARY);
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", uri.toString());

        verify(authorizationManager).hasRightToAnyOrganization(anyCollection());
        verify(terminologyService).resolveTerminology(anySet());
        verify(codeListService).resolveCodelistScheme(anySet());
        verify(modelMapper).mapToJenaModel(any(DataModelDTO.class), any(ModelType.class), any(YtiUser.class));
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(modelMapper).mapToIndexModel(anyString(), any(Model.class));
        verify(openSearchIndexer).createModelToIndex(any(IndexModel.class));
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
        verify(openSearchIndexer).updateModelToIndex(any(IndexModel.class));
    }

    @Test
    void delete() {
        when(coreRepository.graphExists(anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        dataModelService.delete("test");

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).delete(anyString());
        verify(openSearchIndexer).deleteModelFromIndex(anyString());
    }

    @Test
    void deleteNotExists() {
        assertThrows(ResourceNotFoundException.class, () -> dataModelService.delete("test"));
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

        var response = dataModelService.export("test", null, null, "text/turtle");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test:TestClass"));
        assertTrue(response.getBody().contains("test:TestAttribute"));
        assertTrue(response.getBody().contains("test:TestAssociation"));

        //resource
        response = dataModelService.export("test", null, "TestClass", "text/turtle");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test:TestClass"));
        assertFalse(response.getBody().contains("test:TestAttribute"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/ld+json", "text/turtle", "application/rdf+xml"})
    void shouldGetModelWithAcceptHeader(String accept) {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        var response = dataModelService.export("test", null, null, accept);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        //TODO can this be fixed
    }

    @Test
    void shouldRemoveTriplesHiddenFromUnauthenticatedUser() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(false);
        var response = dataModelService.export("test", null, null, "text/turtle");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().contains("skos:editorialNote"));
    }


    @Test
    void testCreateRelease() throws URISyntaxException {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        //remove prior version property since we want to test without
        model.getResource("http://uri.suomi.fi/datamodel/ns/test").removeAll(OWL.priorVersion);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(modelMapper.mapReleaseProperties(any(Model.class), anyString(), anyString(), any(Status.class)))
                .thenReturn("http://uri.suomi.fi/datamodel/ns/test/1.0.1");
        when(modelMapper.mapToIndexModel(anyString(), any(Model.class))).thenReturn(mock(IndexModel.class));

        dataModelService.createRelease("test", "1.0.1", Status.VALID);


        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(modelMapper).mapReleaseProperties(model,
                "http://uri.suomi.fi/datamodel/ns/test",
                "1.0.1",
                Status.VALID);
        verify(modelMapper).mapPriorVersion(any(Model.class),
                                            eq("http://uri.suomi.fi/datamodel/ns/test"),
                                            eq("http://uri.suomi.fi/datamodel/ns/test/1.0.1"));

        verify(coreRepository).put(eq("http://uri.suomi.fi/datamodel/ns/test/1.0.1"), any(Model.class));
        verify(coreRepository).put(eq("http://uri.suomi.fi/datamodel/ns/test"), any(Model.class));

        verify(modelMapper).mapToIndexModel(eq("http://uri.suomi.fi/datamodel/ns/test"), any(Model.class));
        verify(openSearchIndexer).createModelToIndex(any(IndexModel.class));
        verify(visualizationService).saveVersionedPositions("test", "1.0.1");
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
        mockVersionInfo.setVersionIRI("http://uri.suomi.fi/datamodel/ns/test/1.0.2");
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
        verify(openSearchIndexer).updateModelToIndex(any(IndexModel.class));
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
        dataModelDTO.setInternalNamespaces(Set.of("http://uri.suomi.fi/datamodel/ns/test"));
        var extNs = new ExternalNamespaceDTO();
        extNs.setName("test external namespace");
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
