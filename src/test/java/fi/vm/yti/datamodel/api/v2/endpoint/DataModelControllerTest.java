package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.DataModelInfoDTO;
import fi.vm.yti.datamodel.api.v2.dto.ExternalNamespaceDTO;
import fi.vm.yti.datamodel.api.v2.dto.VersionedModelDTO;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.service.ReleaseValidationService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = DataModelController.class)
@ActiveProfiles("junit")
class DataModelControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DataModelService dataModelService;

    @MockBean
    private CoreRepository coreRepository;

    @MockBean
    private ReleaseValidationService releaseValidationService;

    @Autowired
    private DataModelController dataModelController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.dataModelController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(coreRepository.getServiceCategories()).thenReturn(MapperTestUtils.getMockGroups());
        when(coreRepository.getOrganizations()).thenReturn(MapperTestUtils.getMockOrganizations());
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var dataModelDTO = createDatamodelDTO(false);

        this.mvc
                .perform(post("/v2/model/library")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isCreated());

        //Check that functions are called
        verify(dataModelService).create(any(DataModelDTO.class), any(GraphType.class));
        verifyNoMoreInteractions(this.dataModelService);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var dataModelDTO = createDatamodelDTO(true);

        this.mvc
                .perform(put("/v2/model/library/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isNoContent());

        //Check that functions are called
        verify(dataModelService).update(anyString(), any(DataModelDTO.class));
        verifyNoMoreInteractions(dataModelService);
    }

    @Test
    void shouldReturnModel() throws Exception {
        when(dataModelService.get(anyString(), eq(null))).thenReturn(new DataModelInfoDTO());
        this.mvc
                .perform(get("/v2/model/test")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        verify(dataModelService).get(anyString(), eq(null));
        verifyNoMoreInteractions(dataModelService);
    }

    @ParameterizedTest
    @MethodSource("provideDataModelInvalidData")
    void shouldInValidate(DataModelDTO dataModelDTO) throws Exception {

        this.mvc
                .perform(post("/v2/model/profile")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldInValidateLibrary() throws Exception {

        DataModelDTO dataModelDTO = createDatamodelDTO(false);

        // code lists are not allowed in libraries
        dataModelDTO.setCodeLists(Set.of("http://uri.suomi.fi/codelist/test"));
        this.mvc
                .perform(post("/v2/model/library")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("provideDataModelUpdateInvalidData")
    void shouldInValidateUpdate(DataModelDTO dataModelDTO) throws Exception {

        this.mvc
                .perform(put("/v2/model/library/test")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"http","test"})
    void shouldCheckFreePrefixWhenExists(String prefix) throws Exception {
        when(dataModelService.exists(anyString())).thenReturn(true);

        this.mvc
                .perform(get("/v2/model/{prefix}/exists", prefix)
                    .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    void shouldCheckFreePrefixWhenNotExist() throws Exception {
        this.mvc
                .perform(get("/v2/model/xyz/exists")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("false")));
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
        dataModelDTO.setOrganizations(Set.of(MapperTestUtils.TEST_ORG_ID));
        dataModelDTO.setInternalNamespaces(Set.of(Constants.DATA_MODEL_NAMESPACE + "test"));
        var extNs = new ExternalNamespaceDTO();
        extNs.setName(Map.of("fi", "test external namespace"));
        extNs.setPrefix("testprefix");
        extNs.setNamespace("http://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(extNs));
        if(!updateModel){
            dataModelDTO.setPrefix("test");
        }
        dataModelDTO.setTerminologies(Set.of("https://iri.suomi.fi/terminology/test/"));
        var linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", "test link"));
        linkDTO.setDescription(Map.of("fi", "link description"));
        linkDTO.setUri("https://example.com");
        dataModelDTO.setLinks(Set.of(linkDTO));
        return dataModelDTO;
    }

    private static Stream<Arguments> provideDataModelInvalidData() {
        //TODO make pair of DTO and expected error message
        var textFieldMaxPlus = ValidationConstants.TEXT_FIELD_MAX_LENGTH + 20;
        var textAreaMaxPlus = ValidationConstants.TEXT_AREA_MAX_LENGTH + 20;
        var emailAreaMaxPlus = ValidationConstants.EMAIL_FIELD_MAX_LENGTH + 20;
        var documentationAreaMaxPlus = ValidationConstants.DOCUMENTATION_MAX_LENGTH + 20;

        var args = new ArrayList<DataModelDTO>();

        var dataModelDTO = createDatamodelDTO(false);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setPrefix("123");
        args.add(dataModelDTO);

        //Prefix over max length
        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setPrefix(RandomStringUtils.randomAlphanumeric(121));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setPrefix(null);
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setOrganizations(Collections.emptySet());
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setOrganizations(Set.of(UUID.randomUUID()));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setGroups(Collections.emptySet());
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setGroups(Set.of("Not real group"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setLanguages(Set.of("Not real lang"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setLabel(Map.of("Not real lang", "label"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setLabel(Collections.emptyMap());
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setLabel(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setDescription(Map.of("Not real lang", "desc"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setDescription(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setInternalNamespaces(Set.of("http://example.com/ns/test"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        var invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(null); // no null name
        invalidExtRes.setPrefix("test");
        invalidExtRes.setNamespace("http:://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(Map.of("fi", "test name"));
        invalidExtRes.setPrefix(null); //no null prefix
        invalidExtRes.setNamespace("http:://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(Map.of("fi", "test name"));
        invalidExtRes.setPrefix("test");
        invalidExtRes.setNamespace(null); //no null namespace
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(Map.of("fi", "this is invalid"));
        //Reserved words, see ValidationConstants.RESERVED_WORDS
        invalidExtRes.setPrefix("urn");
        invalidExtRes.setNamespace("http:://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(Map.of("fi", "this is invalid"));
        //Reserved word, see ValidationConstants.RESERVED_WORDS
        invalidExtRes.setPrefix("rootResource");
        invalidExtRes.setNamespace("http:://example.com/ns/test");
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        invalidExtRes = new ExternalNamespaceDTO();
        invalidExtRes.setName(Map.of("fi", "this is invalid"));
        //uri.suomi.fi cannot be set as external namespace
        invalidExtRes.setPrefix("test");
        invalidExtRes.setNamespace(Constants.DATA_MODEL_NAMESPACE + "test");
        dataModelDTO.setExternalNamespaces(Set.of(invalidExtRes));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setTerminologies(Set.of("http://invalid.url"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setCodeLists(Set.of("http://invalid.url"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setContact(RandomStringUtils.random(emailAreaMaxPlus));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        dataModelDTO.setDocumentation(Map.of("en", RandomStringUtils.random(documentationAreaMaxPlus)));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        var linkDTO = new LinkDTO();
        linkDTO.setName(Map.of());
        linkDTO.setUri("http://example.com");
        dataModelDTO.setLinks(Set.of(linkDTO));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", "link"));
        linkDTO.setUri(null);
        dataModelDTO.setLinks(Set.of(linkDTO));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", "link"));
        linkDTO.setUri("https://example.com");
        linkDTO.setDescription(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        dataModelDTO.setLinks(Set.of(linkDTO));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO(false);
        linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", RandomStringUtils.random(textFieldMaxPlus)));
        linkDTO.setUri("https://example.com");
        dataModelDTO.setLinks(Set.of(linkDTO));
        args.add(dataModelDTO);

        return args.stream().map(Arguments::of);
    }

    private static Stream<Arguments> provideDataModelUpdateInvalidData() {
        var args = new ArrayList<DataModelDTO>();

        //prefix should not be allowed
        var dataModelDTO = createDatamodelDTO(true);
        dataModelDTO.setPrefix("test");
        args.add(dataModelDTO);

        return args.stream().map(Arguments::of);
    }

    @Test
    void shouldDeleteDataModel() throws Exception {
        mvc.perform(delete("/v2/model/test/"))
                .andExpect(status().isOk());

        verify(dataModelService).delete(anyString(), eq(null));
        verifyNoMoreInteractions(dataModelService);
    }


    @Test
    void shouldCreateRelease() throws Exception {
        mvc.perform(post("/v2/model/test/release")
                        .param("status", "VALID")
                        .param("version","1.0.1"))
                .andExpect(status().isCreated());

        verify(dataModelService).createRelease("test", "1.0.1", Status.VALID);
        verifyNoMoreInteractions(dataModelService);
    }

    @Test
    void shouldGetPriorVersions() throws Exception {
        mvc.perform(get("/v2/model/test/versions")
                        .param("version","1.0.1"))
                .andExpect(status().isOk());

        verify(dataModelService).getPriorVersions("test", "1.0.1");
        verifyNoMoreInteractions(dataModelService);
    }

    @Test
    void shouldValidateAndUpdateVersionedModel() throws Exception {
        var dto = new VersionedModelDTO();
        dto.setDocumentation(Map.of("fi", "test"));
        dto.setStatus(Status.DRAFT);

        mvc.perform(put("/v2/model/test/version")
                .contentType("application/json")
                .content(EndpointUtils.convertObjectToJsonString(dto))
                .param("version", "1.0.1"))
                .andExpect(status().isNoContent());

        verify(dataModelService).updateVersionedModel(eq("test"), eq("1.0.1"), any(VersionedModelDTO.class));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdateVersionedModelDTO")
    void shouldInvalidateUpdateVersionedModel(VersionedModelDTO dto, String[] expectedResult) throws Exception {
        mvc.perform(put("/v2/model/test/version")
                        .contentType("application/json")
                        .content(EndpointUtils.convertObjectToJsonString(dto))
                        .param("version", "1.0.1"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        (res) -> {
                            var errors = res.getResolvedException() != null ?
                                    res.getResolvedException().getMessage().split(", ") :
                                    new String[]{};
                            assertArrayEquals(
                                    Arrays.stream(expectedResult).sorted().toArray(String[]::new),
                                    Arrays.stream(errors).sorted().toArray(String[]::new));
                        }
                );
    }

    private static Stream<Arguments> provideInvalidUpdateVersionedModelDTO() {
        var args = new ArrayList<Pair<VersionedModelDTO, String[]>>();

        var dto = new VersionedModelDTO();
        var expected = new String[]{"updateVersionedModel.dto.status: should-have-value"};
        dto.setDocumentation(Map.of("fi", "test"));
        //dont set status
        args.add(Pair.of(dto, expected));

        //documentation over character limit
        dto = new VersionedModelDTO();
        expected = new String[]{"updateVersionedModel.dto.documentation: value-over-character-limit.50000"};
        dto.setDocumentation(Map.of("fi", RandomStringUtils.randomAlphanumeric(
                ValidationConstants.DOCUMENTATION_MAX_LENGTH + 1)));
        dto.setStatus(Status.VALID);
        args.add(Pair.of(dto, expected));

        return args.stream().map((pair) ->
                Arguments.of(pair.getLeft(), pair.getRight()));
    }
}
