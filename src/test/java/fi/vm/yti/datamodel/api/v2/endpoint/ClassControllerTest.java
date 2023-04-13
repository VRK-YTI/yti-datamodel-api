package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    private final Consumer<ResourceInfoBaseDTO> userMapper = (var dto) -> {};

    private final Consumer<ClassInfoDTO> conceptMapper = (var dto) -> {};

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
        when(terminologyService.mapConceptToClass()).thenReturn(conceptMapper);
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var classDTO = createClassDTO(false);
        var mockModel = mock(Model.class);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classMapper.when(() -> ClassMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class))).thenReturn("test");
            this.mvc
                    .perform(put("/v2/class/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isOk());

            //Check that functions are called
            verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            verify(terminologyService).resolveConcept(anyString());
            classMapper.verify(() -> ClassMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer)
                    .createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldValidateAndCreateMininalClass() throws Exception {
        var classDTO = new ClassDTO();
        classDTO.setIdentifier("Identifier");
        classDTO.setStatus(Status.DRAFT);
        classDTO.setLabel(Map.of("fi", "test"));
        var mockModel = mock(Model.class);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classMapper.when(() -> ClassMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class))).thenReturn("test");
            this.mvc
                    .perform(put("/v2/class/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isOk());

            //Check that functions are called
            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(jenaService).getDataModel(anyString());
            classMapper.verify(() -> ClassMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            resourceMapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer)
                    .createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldNotFindModel() throws Exception {
        var classDTO = createClassDTO(false);

        //finding models from jena is not mocked so it should return null and return 404 not found
        this.mvc
                .perform(put("/v2/class/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("provideCreateClassDTOInvalidData")
    void shouldInvalidate(ClassDTO classDTO) throws Exception {
        this.mvc
                .perform(put("/v2/class/test")
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
        var mockModel = mock(Model.class);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(mockModel.getResource(anyString())).thenReturn(mock(Resource.class));
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);

        try(var resourceMapper = mockStatic(ResourceMapper.class);
            var classMapper = mockStatic(ClassMapper.class)) {
            resourceMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            //mockito will use our mocked static even though we do not call when(). so nothing is needed here

            this.mvc
                    .perform(put("/v2/class/test/class")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                    .andExpect(status().isOk());

            //Check that functions are called
            verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
            verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            classMapper.verify(() -> ClassMapper.mapToUpdateClass(any(Model.class), anyString(), any(Resource.class), any(ClassDTO.class), any(YtiUser.class)));
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
                .perform(put("/v2/class/test/class")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("provideUpdateClassDTOInvalidData")
    void shouldInvalidateUpdate(ClassDTO classDTO) throws Exception{
        this.mvc
                .perform(put("/v2/class/test/class")
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
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/models/test_datamodel_with_resources.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(m);
        when(jenaService.getOrganizations()).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(groupManagementService.mapUser()).thenReturn(userMapper);
        try(var mapper = mockStatic(ClassMapper.class)) {
            mapper.when(() -> ClassMapper.mapToClassDTO(any(Model.class), anyString(), anyString(), any(Model.class), anyBoolean(), eq(userMapper)))
                    .thenReturn(new ClassInfoDTO());
            mvc.perform(get("/v2/class/test/TestClass"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldResourceNotExistGet() throws Exception {
        mvc.perform(get("/v2/class/test/class"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldModelNotExistGet() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);

        mvc.perform(get("/v2/class/test/class"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteClass() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);


        mvc.perform(delete("/v2/class/test/class"))
                .andExpect(status().isOk());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(jenaService).deleteResource(anyString());
        verify(openSearchIndexer).deleteResourceFromIndex(anyString());
    }

    @Test
    void shouldFailToFindClassDelete() throws Exception {
        mvc.perform(delete("/v2/class/test/class"))
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

        mvc.perform(delete("/v2/class/test/class"))
                .andExpect(status().isUnauthorized());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verifyNoMoreInteractions(jenaService);
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verifyNoInteractions(openSearchIndexer);
    }

    private static ClassDTO createClassDTO(boolean update){
        var dto = new ClassDTO();
        dto.setEditorialNote("test comment");
        if(!update){
            dto.setIdentifier("Identifier");
        }
        dto.setStatus(Status.DRAFT);
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setEquivalentClass(Set.of("tietomallit.suomi.fi/ns/notrealns/FakeClass"));
        dto.setSubClassOf(Set.of("tietomallit.suomi.fi/ns/notrealns/FakeClass"));
        dto.setNote(Map.of("fi", "test note"));
        return dto;
    }


}
