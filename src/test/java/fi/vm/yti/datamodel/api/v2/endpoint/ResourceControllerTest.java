package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
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
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(ResourceController.class)
@ActiveProfiles("junit")
class ResourceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JenaService jenaService;

    @MockBean
    private AuthorizationManager authorizationManager;

    @MockBean
    private OpenSearchIndexer openSearchIndexer;

    @MockBean
    private AuthenticatedUserProvider userProvider;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private TerminologyService terminologyService;

    @Autowired
    private ResourceController resourceController;

    private final Consumer<ResourceCommonDTO> userMapper = (var dto) -> {};
    private final Consumer<ResourceInfoBaseDTO> conceptMapper = (var dto) -> {};

    private static final YtiUser USER = EndpointUtils.mockUser;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.resourceController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(authorizationManager.hasRightToAnyOrganization(anyCollection())).thenReturn(true);
        when(authorizationManager.hasRightToModel(any(), any())).thenReturn(true);
        when(userProvider.getUser()).thenReturn(USER);
        when(groupManagementService.mapUser()).thenReturn(userMapper);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);
    }

    @ParameterizedTest
    @CsvSource({"attribute", "association"})
    void shouldValidateAndCreate(String resourceType) throws Exception {
        var resourceDTO = createResourceDTO(false, resourceType);
        var model = EndpointUtils.getMockModel(OWL.Ontology);

        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean())).thenReturn(true);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToResource(anyString(), any(Model.class), any(ResourceDTO.class), any(ResourceType.class), any(YtiUser.class))).thenReturn("test");
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            this.mvc
                    .perform(post("/v2/resource/library/test/{resourceType}", resourceType)
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                    .andExpect(status().isCreated());
            verify(this.jenaService, times(2)).doesResolvedNamespaceExist(anyString());
            verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            if(resourceType.equals("association")){
                verify(jenaService, times(2)).checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean());
            }else{
                verify(jenaService, times(1)).checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean());
            }
            verify(terminologyService).resolveConcept(resourceDTO.getSubject());
            mapper.verify(() -> ResourceMapper.mapToResource(anyString(), any(Model.class), any(ResourceDTO.class), any(ResourceType.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            mapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @ParameterizedTest
    @CsvSource({"attribute", "association"})
    void shouldValidateAndCreateMinimalResource(String resourceType) throws Exception {
        var resourceDTO = new ResourceDTO();
        resourceDTO.setIdentifier("Identifier");
        resourceDTO.setStatus(Status.DRAFT);
        resourceDTO.setLabel(Map.of("fi", "test"));

        var model = EndpointUtils.getMockModel(OWL.Ontology);

        when(jenaService.getDataModel(anyString())).thenReturn(model);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToResource(anyString(), any(Model.class), any(ResourceDTO.class), any(ResourceType.class), any(YtiUser.class))).thenReturn("test");
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            this.mvc
                    .perform(post("/v2/resource/library/test/{resourceType}", resourceType)
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                    .andExpect(status().isCreated());

            //Check that functions are called
            verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            mapper.verify(() -> ResourceMapper.mapToResource(anyString(), any(Model.class), any(ResourceDTO.class), any(ResourceType.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            mapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @ParameterizedTest
    @CsvSource({"attribute", "association"})
    void shouldNotFindModel(String resourceType) throws Exception {
        var resourceDTO = createResourceDTO(false, resourceType);
        var updateDTO = createResourceDTO(true, resourceType);
        // var model = EndpointUtils.getMockModel(DCAP.DCAP);
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean())).thenReturn(true);

        doThrow(ResourceNotFoundException.class).when(jenaService).getDataModel(anyString());
        //NOTE for this test. This should probably never happen in any kind of situation.
        // but the controller can catch it if it happens


        //finding models from jena is not mocked so it should return null and return 404 not found
        this.mvc
                .perform(post("/v2/resource/library/test/{resourceType}", resourceType)
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isNotFound());

        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);

        this.mvc
                .perform(put("/v2/resource/library/test/{resourceType}/resource", resourceType)
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(updateDTO)))
                .andExpect(status().isNotFound());

        this.mvc
                .perform(get("/v2/resource/library/test/resource")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"attribute", "association"})
    void resourceShouldAlreadyExist(String resourceType) throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean())).thenReturn(true);
        var resourceDTO = createResourceDTO(false, resourceType);

        //finding models from jena is not mocked so it should return null and return 404 not found
        this.mvc
                .perform(post("/v2/resource/library/test/{resourceType}", resourceType)
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"status\":\"BAD_REQUEST\",\"message\":\"Error during mapping: Already exists\"}"));
    }

    @ParameterizedTest
    @MethodSource("provideCreateResourceDTOInvalidData")
    void shouldInvalidate(String resourceType, ResourceDTO resourceDTO) throws Exception {
        this.mvc
                .perform(post("/v2/resource/library/test/{resourceType}", resourceType)
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> provideCreateResourceDTOInvalidData() {
        var args = new ArrayList<Arguments>();
        var textAreaMaxPlus = ValidationConstants.TEXT_AREA_MAX_LENGTH + 20;

        var testType = "association";
        var resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setStatus(null);
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setLabel(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setLabel(Map.of("fi", " "));
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setEditorialNote(RandomStringUtils.random(textAreaMaxPlus));
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setNote(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setIdentifier(null);
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, testType);
        resourceDTO.setDomain("http://uri.suomi.fi/datamodel/ns/int/InvalidClass");
        args.add(Arguments.of(testType, resourceDTO));

        resourceDTO = createResourceDTO(false, "attribute");
        resourceDTO.setRange("notreal:type");
        args.add(Arguments.of("attribute", resourceDTO));

        resourceDTO = createResourceDTO(false, "association");
        resourceDTO.setRange("http://uri.suomi.fi/datamodel/ns/int/InvalidClass");
        args.add(Arguments.of("association", resourceDTO));

        return args.stream();
    }

    @ParameterizedTest
    @CsvSource({"attribute", "association"})
    void shouldValidateAndUpdate(String resourceType) throws Exception {
        var resourceDTO = createResourceDTO(true, resourceType);
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean())).thenReturn(true);

        when(jenaService.getDataModel(anyString())).thenReturn(m);
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        this.mvc
                .perform(put("/v2/resource/library/test/{resourceType}/TestA{resourceType}", resourceType, resourceType.substring(1))
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isNoContent());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(jenaService).putDataModelToCore(anyString(), any(Model.class));
        verify(openSearchIndexer).updateResourceToIndex(any(IndexResource.class));
    }

    @ParameterizedTest
    @CsvSource({"association", "attribute"})
    void shouldNotFindResource(String resourceType) throws Exception {
        var resourceDTO = createResourceDTO(true, resourceType);
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(false);
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(), anyBoolean())).thenReturn(true);

        this.mvc
                .perform(put("/v2/resource/library/test/{resourceType}/resource", resourceType)
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("provideUpdateResourceDTOInvalidData")
    void shouldInvalidateUpdate(ResourceDTO resourceDTO) throws Exception{
        this.mvc
                .perform(put("/v2/resource/library/test/association/resource")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(resourceDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetResource() throws Exception {
        var model = EndpointUtils.getMockModel(OWL.Ontology);
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(jenaService.getOrganizations()).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToResourceInfoDTO(any(Model.class), anyString(), anyString(), any(Model.class), anyBoolean(), eq(userMapper)))
                    .thenReturn(new ResourceInfoDTO());
            mvc.perform(get("/v2/resource/library/test/TestAttribute"))
                    .andExpect(status().isOk());
            verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(jenaService).getDataModel(anyString());
            verify(jenaService).getOrganizations();
            mapper.verify(() -> ResourceMapper.mapToResourceInfoDTO(any(Model.class), anyString(), anyString(), any(Model.class), anyBoolean(), eq(userMapper)));
            verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        }
    }

    @Test
    void shouldNotFindResourceGet() throws Exception {
        mvc.perform(get("/v2/resource/library/test/resource"))
                .andExpect(status().isNotFound());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
    }

    @Test
    void shouldDeleteResource() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);
        when(jenaService.getDataModel(anyString())).thenReturn(mock(Model.class));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);


        mvc.perform(delete("/v2/resource/library/test/resource"))
                .andExpect(status().isOk());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(jenaService).deleteResource(anyString());
        verify(openSearchIndexer).deleteResourceFromIndex(anyString());
    }

    @Test
    void shouldFailToFindResourceDelete() throws Exception {
        mvc.perform(delete("/v2/resource/library/test/resource"))
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

        mvc.perform(delete("/v2/resource/library/test/resource"))
                .andExpect(status().isUnauthorized());

        verify(jenaService).doesResourceExistInGraph(anyString(), anyString());
        verify(jenaService).getDataModel(anyString());
        verifyNoMoreInteractions(jenaService);
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verifyNoInteractions(openSearchIndexer);
    }

    @Test
    void shouldValidateAndCreatePropertyShape() throws Exception {
        var dto = createPropertyShapeDTO();
        var model = EndpointUtils.getMockModel(DCAP.DCAP);

        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(jenaService.checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"), anyList(),
                anyBoolean())).thenReturn(true);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToPropertyShapeResource(anyString(), any(Model.class),
                    any(PropertyShapeDTO.class), any(YtiUser.class))).thenReturn("test");
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class),
                    anyString())).thenReturn(new IndexResource());
            this.mvc
                    .perform(post("/v2/resource/profile/test")
                            .contentType("application/json")
                            .content(EndpointUtils.convertObjectToJsonString(dto)))
                    .andExpect(status().isCreated());
            verify(this.jenaService).doesResourceExistInGraph(anyString(), anyString());
            verify(this.jenaService).getDataModel(anyString());
            verify(jenaService, times(2)).checkIfResourceIsOneOfTypes(eq("http://uri.suomi.fi/datamodel/ns/int/FakeClass"),
                    anyList(), anyBoolean());
            verify(terminologyService).resolveConcept(dto.getSubject());
            mapper.verify(() -> ResourceMapper.mapToPropertyShapeResource(anyString(), any(Model.class),
                    any(PropertyShapeDTO.class), any(YtiUser.class)));
            verify(this.jenaService).putDataModelToCore(anyString(), any(Model.class));
            verifyNoMoreInteractions(this.jenaService);
            mapper.verify(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString()));
            verify(this.openSearchIndexer).createResourceToIndex(any(IndexResource.class));
            verifyNoMoreInteractions(this.openSearchIndexer);
        }
    }

    @Test
    void shouldCopy() throws Exception {
        when(jenaService.doesResourceExistInGraph("http://uri.suomi.fi/datamodel/ns/test", "http://uri.suomi.fi/datamodel/ns/test/PropertyShape")).thenReturn(true);
        var m = ModelFactory.createDefaultModel();
        m.createResource("http://uri.suomi.fi/datamodel/ns/test")
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, Iow.ApplicationProfile);
        m.createResource("http://uri.suomi.fi/datamodel/ns/newtest")
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, Iow.ApplicationProfile);
        when(jenaService.getDataModel(anyString())).thenReturn(m);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class),
                    anyString())).thenReturn(new IndexResource());

            this.mvc
                    .perform(post("/v2/resource/profile/test/PropertyShape")
                            .contentType("application/json")
                            .param("targetPrefix", "newtest")
                            .param("newIdentifier", "newid"))
                    .andExpect(status().isCreated());

            verify(jenaService, times(2)).doesResourceExistInGraph(anyString(), anyString());
            verify(jenaService, times(2)).getDataModel(anyString());
            verify(authorizationManager, times(2)).hasRightToModel(anyString(), any(Model.class));
        }
    }

    @Test
    void shouldNotCopyResourceNotFound() throws Exception {
        this.mvc
                .perform(post("/v2/resource/profile/test/PropertyShape")
                        .contentType("application/json")
                        .param("targetPrefix", "newtest")
                        .param("newIdentifier", "newid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotCopyIdentifierInUse() throws Exception {
        when(jenaService.doesResourceExistInGraph(anyString(), anyString())).thenReturn(true);

        this.mvc
                .perform(post("/v2/resource/profile/test/PropertyShape")
                        .contentType("application/json")
                        .param("targetPrefix", "newtest")
                        .param("newIdentifier", "newid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotCopyModelNotApplicationProfileOne() throws Exception {
        when(jenaService.doesResourceExistInGraph("http://uri.suomi.fi/datamodel/ns/test", "http://uri.suomi.fi/datamodel/ns/test/PropertyShape")).thenReturn(true);
        var modelLibrary = ModelFactory.createDefaultModel();
        modelLibrary.createResource("http://uri.suomi.fi/datamodel/ns/test")
                        .addProperty(RDF.type, OWL.Ontology);
        var modelProfile = ModelFactory.createDefaultModel();
        modelProfile.createResource("http://uri.suomi.fi/datamodel/ns/newtest")
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, Iow.ApplicationProfile);
        when(jenaService.getDataModel("http://uri.suomi.fi/datamodel/ns/test")).thenReturn(modelLibrary);
        when(jenaService.getDataModel("http://uri.suomi.fi/datamodel/ns/newtest")).thenReturn(modelProfile);

        this.mvc
                .perform(post("/v2/resource/profile/test/PropertyShape")
                        .contentType("application/json")
                        .param("targetPrefix", "newtest")
                        .param("newIdentifier", "newid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotCopyModelNotApplicationProfileBoth() throws Exception {
        when(jenaService.doesResourceExistInGraph("http://uri.suomi.fi/datamodel/ns/test", "http://uri.suomi.fi/datamodel/ns/test/PropertyShape")).thenReturn(true);
        var test = ModelFactory.createDefaultModel();
        test.createResource("http://uri.suomi.fi/test")
                .addProperty(RDF.type, OWL.Ontology);
        var newtest = ModelFactory.createDefaultModel();
        newtest.createResource("http://uri.suomi.fi/newtest")
                .addProperty(RDF.type, OWL.Ontology);
        when(jenaService.getDataModel("http://uri.suomi.fi/datamodel/ns/test")).thenReturn(test);
        when(jenaService.getDataModel("http://uri.suomi.fi/datamodel/ns/newtest")).thenReturn(newtest);

        this.mvc
                .perform(post("/v2/resource/profile/test/PropertyShape")
                        .contentType("application/json")
                        .param("targetPrefix", "newtest")
                        .param("newIdentifier", "newid"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("provideCreatePropertyShapeDTOInvalidData")
    void shouldInvalidatePropertyShape(PropertyShapeDTO dto) throws Exception {
        this.mvc
                .perform(post("/v2/resource/profile/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldCheckFreeIdentifierWhenExists() throws Exception {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test";
        when(jenaService.doesResourceExistInGraph(graphUri, graphUri + ModelConstants.RESOURCE_SEPARATOR + "Resource")).thenReturn(true);

        this.mvc
                .perform(get("/v2/resource/test/Resource/exists")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    void shouldCheckFreeIdentifierWhenNotExist() throws Exception {
        when(jenaService.doesDataModelExist(anyString())).thenReturn(false);

        this.mvc
                .perform(get("/v2/resource/test/Resource/exists")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("false")));
    }


    private static Stream<Arguments> provideCreatePropertyShapeDTOInvalidData() {
        var args = new ArrayList<PropertyShapeDTO>();
        var length = ValidationConstants.TEXT_FIELD_MAX_LENGTH;

        var dto = createPropertyShapeDTO();
        dto.setPath("http://uri.suomi.fi/invalid");
        args.add(dto);

        dto = createPropertyShapeDTO();
        dto.setDataType("invalid");
        args.add(dto);

        dto = createPropertyShapeDTO();
        dto.setDefaultValue(RandomStringUtils.random(length + 1));
        args.add(dto);

        dto = createPropertyShapeDTO();
        dto.setHasValue(RandomStringUtils.random(length + 1));
        args.add(dto);

        dto = createPropertyShapeDTO();
        dto.setAllowedValues(List.of(RandomStringUtils.random(length + 1)));
        args.add(dto);

        dto = createPropertyShapeDTO();
        dto.setCodeList("http://uri.suomi.fi/invalid");
        args.add(dto);

        return args.stream().map(Arguments::of);
    }
    private static Stream<Arguments> provideUpdateResourceDTOInvalidData() {
        var args = new ArrayList<ResourceDTO>();

        //this has identifier so it should fail automatically
        var resourceDTO = createResourceDTO(false, "association");
        args.add(resourceDTO);

        return args.stream().map(Arguments::of);
    }

    private static ResourceDTO createResourceDTO(boolean update, String resourceType){
        var dto = new ResourceDTO();
        dto.setEditorialNote("test comment");
        if(!update){
            dto.setIdentifier("Identifier");
        }
        dto.setStatus(Status.DRAFT);
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/FakeResource"));
        dto.setSubResourceOf(Set.of("http://uri.suomi.fi/datamodel/ns/int/FakeResource"));
        dto.setDomain("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        if(resourceType.equals("association")){
            dto.setRange("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        }else{
            dto.setRange("owl:real");
        }
        dto.setNote(Map.of("fi", "test note"));
        return dto;
    }

    private static PropertyShapeDTO createPropertyShapeDTO() {
        var dto = new PropertyShapeDTO();
        dto.setLabel(Map.of("fi", "test label"));
        dto.setIdentifier("Identifier");
        dto.setType(ResourceType.ASSOCIATION);
        dto.setStatus(Status.DRAFT);
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setPath("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        dto.setClassType("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        return dto;
    }
}
