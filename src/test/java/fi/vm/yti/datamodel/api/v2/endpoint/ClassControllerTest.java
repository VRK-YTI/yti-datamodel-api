package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexClass;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
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
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ClassMapper classMapper;

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
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var classDTO = createClassDTO(false);
        var mockModel = mock(Model.class);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(classMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class))).thenReturn("test");
        when(classMapper.mapToIndexClass(any(Model.class), anyString())).thenReturn(mock(IndexClass.class));

        this.mvc
                .perform(put("/v2/class/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isOk());

        //Check that functions are called
        verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
        verify(this.jenaService).getDataModel(anyString());
        verify(this.classMapper)
                .createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class));
        verify(this.classMapper)
                .mapToIndexClass(eq(mockModel), anyString());
        verifyNoMoreInteractions(this.classMapper);
        verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
        verifyNoMoreInteractions(this.jenaService);
        verify(this.openSearchIndexer)
                .createClassToIndex(any(IndexClass.class));
        verifyNoMoreInteractions(this.openSearchIndexer);
    }

    @Test
    void shouldValidateAndCreateMininalClass() throws Exception {
        var classDTO = new ClassDTO();
        classDTO.setIdentifier("Identifier");
        classDTO.setStatus(Status.DRAFT);
        classDTO.setLabel(Map.of("fi", "test"));
        var mockModel = mock(Model.class);

        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(classMapper.createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class))).thenReturn("test");
        when(classMapper.mapToIndexClass(any(Model.class), anyString())).thenReturn(mock(IndexClass.class));

        this.mvc
                .perform(put("/v2/class/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isOk());

        //Check that functions are called
        verify(jenaService).getDataModel(anyString());
        verify(this.classMapper)
                .createClassAndMapToModel(anyString(), any(Model.class), any(ClassDTO.class));
        verify(this.classMapper)
                .mapToIndexClass(eq(mockModel), anyString());
        verifyNoMoreInteractions(this.classMapper);
        verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
        verifyNoMoreInteractions(this.jenaService);
        verify(this.openSearchIndexer)
                .createClassToIndex(any(IndexClass.class));
        verifyNoMoreInteractions(this.openSearchIndexer);
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
        when(classMapper.mapToIndexClass(any(Model.class), anyString())).thenReturn(mock(IndexClass.class));

        this.mvc
                .perform(put("/v2/class/test/class")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(classDTO)))
                .andExpect(status().isOk());

        //Check that functions are called
        verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
        verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(this.jenaService).getDataModel(anyString());
        verify(this.classMapper)
                .mapToUpdateClass(any(Model.class), anyString(), any(Resource.class), any(ClassDTO.class));
        verify(this.classMapper)
                .mapToIndexClass(eq(mockModel), anyString());
        verifyNoMoreInteractions(this.classMapper);
        verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
        verifyNoMoreInteractions(this.jenaService);
        verify(this.openSearchIndexer)
                .updateClassToIndex(any(IndexClass.class));
        verifyNoMoreInteractions(this.openSearchIndexer);
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
