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
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

        verify(coreRepository).fetch(ModelConstants.SUOMI_FI_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR);
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(modelMapper).mapToDataModelDTO(anyString(), any(Model.class), eq(null));
    }

    @Test
    void getWithVersion() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        dataModelService.get("test", "1.0.1");

        verify(coreRepository).fetch(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1" + ModelConstants.RESOURCE_SEPARATOR);
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
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", uri.toString());

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
        var mockUser = spy(EndpointUtils.mockSuperUser);
        when(userProvider.getUser()).thenReturn(mockUser);
        dataModelService.delete("test", null);

        verify(coreRepository).graphExists(anyString());
        verify(mockUser).isSuperuser();
        verify(coreRepository).delete(anyString());
        verify(openSearchIndexer).deleteModelFromIndex(anyString());
        verify(openSearchIndexer).removeResourceIndexesByDataModel(anyString(), eq(null));
    }

    @Test
    void deleteVersionedModel() {
        when(coreRepository.graphExists(anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        var mockUser = spy(EndpointUtils.mockSuperUser);
        when(userProvider.getUser()).thenReturn(mockUser);
        dataModelService.delete("test", "1.0.1");

        verify(coreRepository).graphExists(anyString());
        verify(mockUser).isSuperuser();
        verify(coreRepository).delete(anyString());
        verify(openSearchIndexer).deleteModelFromIndex(anyString());
        verify(openSearchIndexer).removeResourceIndexesByDataModel(anyString(), eq("1.0.1"));
    }

    @Test
    void deleteNotSuperUser() {
        when(userProvider.getUser()).thenReturn(mock(YtiUser.class));
        assertThrows(AuthorizationException.class, () -> dataModelService.delete("test", null));
        assertThrows(AuthorizationException.class, () -> dataModelService.delete("test", "1.0.1"));
    }

    @Test
    void deleteNotExists() {
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockSuperUser);

        assertThrows(ResourceNotFoundException.class, () -> dataModelService.delete("test", null));

        verify(coreRepository, never()).delete(anyString());
        verifyNoInteractions(openSearchIndexer);
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

        var response = dataModelService.export("test", null, "text/turtle", false);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test:TestClass"));
        assertTrue(response.getBody().contains("test:TestAttribute"));
        assertTrue(response.getBody().contains("test:TestAssociation"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/ld+json", "text/turtle", "application/rdf+xml"})
    void shouldGetModelWithAcceptHeader(String accept) {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());

        var response = dataModelService.export("test", null, accept, false);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        //TODO can this be fixed
    }

    @Test
    void shouldRemoveTriplesHiddenFromUnauthenticatedUser() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(false);
        var response = dataModelService.export("test", null, "text/turtle", false);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().contains("skos:editorialNote"));
    }


    @Test
    void testCreateRelease() throws URISyntaxException {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        //remove prior version property since we want to test without
        model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test").removeAll(OWL.priorVersion);
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
                                            eq(ModelConstants.SUOMI_FI_NAMESPACE + "test/"),
                                            eq(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/"));

        verify(coreRepository).put(eq(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/"), any(Model.class));
        verify(coreRepository).put(eq(ModelConstants.SUOMI_FI_NAMESPACE + "test/"), any(Model.class));

        verify(modelMapper).mapToIndexModel(eq(ModelConstants.SUOMI_FI_NAMESPACE + "test/"), any(Model.class));
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
        mockVersionInfo.setVersionIRI(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.2");
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

    @Test
    void testValidateRelease() {
        var dataModelURI = DataModelURI.createResourceURI("test", "incomplete");
        var model = ModelFactory.createDefaultModel();
        model.createResource(dataModelURI.getModelURI())
                        .addProperty(RDF.type, SuomiMeta.ApplicationProfile);

        model.createResource(dataModelURI.getResourceURI())
                        .addProperty(RDFS.label, "incomplete resource")
                        .addProperty(SH.targetClass, OWL.Thing);

        when(coreRepository.fetch(dataModelURI.getGraphURI())).thenReturn(model);

        var validationResult = dataModelService.validateRelease("test");

        var errors = validationResult.get("missing-reference-to-library");
        var error = errors.iterator().next();

        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertEquals("incomplete resource", error.getLabel().get("en"));
        assertEquals("https://iri.suomi.fi/model/test/incomplete", error.getUri());
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
        dataModelDTO.setInternalNamespaces(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "test"));
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
