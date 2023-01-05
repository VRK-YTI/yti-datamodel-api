package fi.vm.yti.datamodel.api.v2.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.ElasticIndexer;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ExceptionHandlerAdvice;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers =Datamodel.class)
@ActiveProfiles("junit")
class DatamodelTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JenaService jenaService;

    @MockBean
    private AuthorizationManager authorizationManager;

    @MockBean
    private ElasticIndexer elasticIndexer;

    @MockBean
    private ModelMapper modelMapper;

    @Autowired
    private Datamodel datamodel;

    private static final UUID RANDOM_ORG = UUID.randomUUID();


    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.datamodel)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();

        when(authorizationManager.hasRightToAnyOrganization(anyCollection())).thenReturn(true);
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var dataModelDTO = createDatamodelDTO();
        //Mock validation stuff
        var mockModel = ModelFactory.createDefaultModel();
        var res = mockModel.createResource("urn:uuid:" + RANDOM_ORG);
        res.addProperty(SKOS.notation, "P11");
        when(jenaService.getOrganizations()).thenReturn(mockModel);
        when(jenaService.getServiceCategories()).thenReturn(mockModel);

        //Mock mapping
        var model = mock(Model.class);
        var indexmodel = mock(IndexModel.class);
        when(modelMapper.mapToJenaModel(any(DataModelDTO.class))).thenReturn(model);
        when(modelMapper.mapToIndexModel("test", model)).thenReturn(indexmodel);

        this.mvc
                .perform(put("/v2/model")
                        .contentType("application/json")
                        .content(convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isOk());

        //Check that functions are called
        verify(this.modelMapper)
                .mapToJenaModel(any(DataModelDTO.class));
        verify(this.modelMapper)
                .mapToIndexModel(anyString(), any(Model.class));
        verifyNoMoreInteractions(this.modelMapper);
        verify(this.elasticIndexer)
                .createModelToIndex(any(IndexModel.class));
        verifyNoMoreInteractions(this.elasticIndexer);
    }

    @Test
    void shouldReturnModel() throws Exception {
        var mockModel = mock(Model.class);
        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        when(modelMapper.mapToDataModelDTO(anyString(), any(Model.class))).thenReturn(new DataModelDTO());

        this.mvc
                .perform(get("/v2/model/test")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        verify(this.jenaService)
                .getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + "test");
        verifyNoMoreInteractions(this.jenaService);
        verify(modelMapper)
                .mapToDataModelDTO(eq("test"), any(Model.class));
        verifyNoMoreInteractions(this.modelMapper);
    }

    @ParameterizedTest
    @MethodSource("provideDataModelInvalidData")
    void shouldInValidate(DataModelDTO dataModelDTO) throws Exception {
        //Mock validation stuff
        var mockModel = ModelFactory.createDefaultModel();
        var res = mockModel.createResource("urn:uuid:" + RANDOM_ORG);
        res.addProperty(SKOS.notation, "P11");
        when(jenaService.getOrganizations()).thenReturn(mockModel);
        when(jenaService.getServiceCategories()).thenReturn(mockModel);

        //Mock mapping
        var model = mock(Model.class);
        var indexmodel = mock(IndexModel.class);
        when(modelMapper.mapToJenaModel(dataModelDTO)).thenReturn(model);
        when(modelMapper.mapToIndexModel("test", model)).thenReturn(indexmodel);

        this.mvc
                .perform(put("/v2/model")
                        .contentType("application/json")
                        .content(convertObjectToJsonString(dataModelDTO)))
                .andExpect(status().isBadRequest());
    }

    private String convertObjectToJsonString(DataModelDTO datamodel) throws JsonProcessingException {
        var mapper = new ObjectMapper();
        return mapper.writeValueAsString(datamodel);
    }


    private static DataModelDTO createDatamodelDTO(){
        DataModelDTO dataModelDTO = new DataModelDTO();
        dataModelDTO.setDescription(Map.of("fi", "test description"));
        dataModelDTO.setLabel(Map.of("fi", "test label"));
        dataModelDTO.setGroups(Set.of("P11"));
        dataModelDTO.setLanguages(Set.of("fi"));
        dataModelDTO.setOrganizations(Set.of(RANDOM_ORG));
        dataModelDTO.setPrefix("test");
        dataModelDTO.setStatus(Status.DRAFT);
        dataModelDTO.setType(ModelType.LIBRARY);
        return dataModelDTO;
    }

    private static Stream<Arguments> provideDataModelInvalidData() {
        var textAreaMaxPlus = ValidationConstants.TEXT_AREA_MAX_LENGTH + 20;

        var args = new ArrayList<DataModelDTO>();

        // without a prefLabel
        var dataModelDTO = createDatamodelDTO();
        dataModelDTO.setPrefix("123");
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setPrefix("asd123asd1232asd123");
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setOrganizations(Collections.emptySet());
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setOrganizations(Set.of(UUID.randomUUID()));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setGroups(Collections.emptySet());
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setGroups(Set.of("Not real group"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setLanguages(Set.of("Not real lang"));
        args.add(dataModelDTO);


        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setLabel(Map.of("Not real lang", "label"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setLabel(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setDescription(Map.of("Not real lang", "desc"));
        args.add(dataModelDTO);

        dataModelDTO = createDatamodelDTO();
        dataModelDTO.setDescription(Map.of("fi", RandomStringUtils.random(textAreaMaxPlus)));
        args.add(dataModelDTO);

        return args.stream().map(Arguments::of);
    }
}
