package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.SearchIndexService;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(ClassController.class)
@ActiveProfiles("junit")
class ClassControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JenaService jenaService;

    @MockBean
    private AuthorizationManager authorizationManager;

    @MockBean
    private OpenSearchIndexer openSearchIndexer;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private AuthenticatedUserProvider userProvider;

    @MockBean
    private TerminologyService terminologyService;

    @MockBean
    private SearchIndexService searchIndexService;

    private final Consumer<ResourceCommonDTO> userMapper = (var dto) -> {};

    private final Consumer<ResourceInfoBaseDTO> conceptMapper = (var dto) -> {};

    private static final YtiUser USER = EndpointUtils.mockUser;

    @Autowired
    private ClassController classController;


    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.classController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(authorizationManager.hasRightToAnyOrganization(anyCollection())).thenReturn(true);
        when(authorizationManager.hasRightToModel(any(), any())).thenReturn(true);
        when(userProvider.getUser()).thenReturn(USER);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);
    }

    @Test
    void shouldValidateAndCreateClass() throws Exception {
        var classDTO = createClassDTO(false);
        Model mockModel = EndpointUtils.getMockModel(OWL.Ontology);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classMapper.when(() -> ClassMapper.createOntologyClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class))).thenReturn("test");
            this.mvc
                    .perform(post("/v2/class/library/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isCreated());

            //Check that functions are called
            verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            verify(terminologyService).resolveConcept(anyString());
            classMapper.verify(() -> ClassMapper.createOntologyClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldValidateAndCreateMinimalClass() throws Exception {
        var classDTO = new ClassDTO();
        classDTO.setIdentifier("Identifier");
        classDTO.setStatus(Status.DRAFT);
        classDTO.setLabel(Map.of("fi", "test"));
        Model mockModel = EndpointUtils.getMockModel(OWL.Ontology);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classMapper.when(() -> ClassMapper.createOntologyClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class))).thenReturn("test");
            this.mvc
                    .perform(post("/v2/class/library/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isCreated());

            //Check that functions are called
            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(jenaService).getDataModel(anyString());
            classMapper.verify(() -> ClassMapper.createOntologyClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldNotFindModel() throws Exception {
        var classDTO = createClassDTO(false);
        doThrow(ResourceNotFoundException.class).when(jenaService).getDataModel(anyString());

        this.mvc
                .perform(post("/v2/class/library/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("provideCreateClassDTOInvalidData")
    void shouldInvalidate(ClassDTO classDTO) throws Exception {
        this.mvc
                .perform(post("/v2/class/library/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> provideCreateClassDTOInvalidData() {
        var args = new ArrayList<ClassDTO>();
        var textAreaMaxPlus = ValidationConstants.TEXT_AREA_MAX_LENGTH + 20;

        var classDTO = createClassDTO(false);
        classDTO.setStatus(null);
        args.add(classDTO);

        classDTO = createClassDTO(false);
        classDTO.setLabel(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(classDTO);

        classDTO = createClassDTO(false);
        classDTO.setLabel(Map.of("fi", " "));
        args.add(classDTO);

        classDTO = createClassDTO(false);
        classDTO.setEditorialNote(RandomStringUtils.random(textAreaMaxPlus));
        args.add(classDTO);


        classDTO = createClassDTO(false);
        classDTO.setNote(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(classDTO);

        classDTO = createClassDTO(false);
        classDTO.setIdentifier(null);
        args.add(classDTO);

        return args.stream().map(Arguments::of);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var classDTO = createClassDTO(true);
        var mockModel = EndpointUtils.getMockModel(OWL.Ontology);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);

        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            //mockito will use our mocked static even though we do not call when(). so nothing is needed here

            this.mvc
                    .perform(put("/v2/class/library/test/class")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isNoContent());

            //Check that functions are called
            verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
            verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            classMapper.verify(() -> ClassMapper.mapToUpdateOntologyClass(any(Model.class), anyString(), any(Resource.class), any(ClassDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer)
                    .updateResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldNotFindClass() throws Exception {
        var classDTO = createClassDTO(true);
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(false);

        this.mvc
                .perform(put("/v2/class/library/test/class")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("provideUpdateClassDTOInvalidData")
    void shouldInvalidateUpdate(ClassDTO classDTO) throws Exception{
        this.mvc
                .perform(put("/v2/class/library/test/class")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> provideUpdateClassDTOInvalidData() {
        var args = new ArrayList<ClassDTO>();

        //this has identifier so it should fail automatically
        var classDTO = createClassDTO(false);
        args.add(classDTO);

        return args.stream().map(Arguments::of);
    }

    @Test
    void shouldGetClass() throws Exception {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(m);
        when(jenaService.getOrganizations()).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(groupManagementService.mapUser()).thenReturn(userMapper);
        try(var mapper = mockStatic(ClassMapper.class)) {
            mapper.when(() -> ClassMapper.mapToClassDTO(any(Model.class), anyString(), anyString(), any(Model.class), anyBoolean(), eq(userMapper)))
                    .thenReturn(new ClassInfoDTO());
            mvc.perform(get("/v2/class/library/test/TestClass"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldResourceNotExistGet() throws Exception {
        mvc.perform(get("/v2/class/library/test/class"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldModelNotExistGet() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        doThrow(ResourceNotFoundException.class).when(jenaService).getDataModel(anyString());
        mvc.perform(get("/v2/class/library/test/class"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteClass() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);


        mvc.perform(delete("/v2/class/library/test/class"))
                .andExpect(status().isOk());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(jenaService).deleteResource(anyString());
        verify(openSearchIndexer).deleteResourceFromIndex(anyString());
    }

    @Test
    void shouldFailToFindClassDelete() throws Exception {
        mvc.perform(delete("/v2/class/library/test/class"))
                .andExpect(status().isNotFound());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verifyNoMoreInteractions(jenaService);
        verifyNoInteractions(authorizationManager, openSearchIndexer);
    }

    @Test
    void shouldFailAuthorisationDelete() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(false);

        mvc.perform(delete("/v2/class/library/test/class"))
                .andExpect(status().isUnauthorized());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verifyNoMoreInteractions(jenaService);
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verifyNoInteractions(openSearchIndexer);
    }

    @Test
    void shouldValidateAndCreateNodeShape() throws Exception {
        var nodeShapeDTO = createNodeShapeDTO();
        nodeShapeDTO.setProperties(Set.of("test"));
        Model mockModel = EndpointUtils.getMockModel(DCAP.DCAP);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(jenaService.findResources(anySet())).thenReturn(ModelFactory.createDefaultModel());
        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            // Predicate<String> predicate = any();
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classMapper.when(() -> ClassMapper.createNodeShapeAndMapToModel(anyString(), any(Model.class), any(NodeShapeDTO.class), any(YtiUser.class))).thenReturn("test");
            classMapper.when(() -> ClassMapper.mapPlaceholderPropertyShapes(any(Model.class), anyString(), any(Model.class), any(YtiUser.class), any(Predicate.class)))
                    .thenReturn(new ArrayList<>());
            this.mvc
                    .perform(post("/v2/class/profile/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(nodeShapeDTO)))
                    .andExpect(status().isCreated());

            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            verify(terminologyService).resolveConcept(anyString());
            classMapper.verify(() -> ClassMapper.createNodeShapeAndMapToModel(anyString(), any(Model.class), any(NodeShapeDTO.class), any(YtiUser.class)));
            classMapper.verify(() -> ClassMapper.mapPlaceholderPropertyShapes(any(Model.class), anyString(), any(Model.class), any(YtiUser.class), any(Predicate.class)));
            verify(this.jenaService).findResources(anySet());
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer)
                    .bulkInsert(anyString(), anyList());
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldCheckFreeIdentifierWhenExists() throws Exception {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test";
        when(jenaService.doesResourceExistInGraph(graphUri, graphUri + ModelConstants.RESOURCE_SEPARATOR + "Resource")).thenReturn(true);

        this.mvc
                .perform(get("/v2/class/test/Resource/exists")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("false")));
    }

    @Test
    void shouldCheckFreeIdentifierWhenNotExist() throws Exception {
        when(jenaService.doesDataModelExist(anyString())).thenReturn(false);

        this.mvc
                .perform(get("/v2/class/test/Resource/exists")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }


    private static ClassDTO createClassDTO(boolean update){
        var dto = new ClassDTO();
        dto.setEditorialNote("test comment");
        if(!update){
            dto.setIdentifier("Identifier");
        }
        dto.setStatus(Status.DRAFT);
        dto.setSubject("http://uri.suomi.fi/terminology/notrealurl");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setEquivalentClass(Set.of("tietomallit.suomi.fi/ns/notrealns/FakeClass"));
        dto.setSubClassOf(Set.of("tietomallit.suomi.fi/ns/notrealns/FakeClass"));
        dto.setNote(Map.of("fi", "test note"));
        return dto;
    }

    private static NodeShapeDTO createNodeShapeDTO() {
        var dto = new NodeShapeDTO();
        dto.setLabel(Map.of("fi", "node label"));
        dto.setIdentifier("node-shape-1");
        dto.setStatus(Status.DRAFT);
        dto.setSubject("http://uri.suomi.fi/terminology/concept-123");

        return dto;
    }
}
