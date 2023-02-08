package fi.vm.yti.datamodel.api.mapper;


import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import({
        ModelMapper.class
})
class ModelMapperTest {

    @MockBean
    JenaService jenaService;
    @Autowired
    ModelMapper mapper;

    @BeforeEach
    public void init(){
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource("urn:uuid:7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");
        var groupRes = mockModel.createResource("http://urn.fi/URN:NBN:fi:au:ptvl:v1105");
        groupRes.addProperty(SKOS.notation, "P11");
        when(jenaService.getOrganizations()).thenReturn(mockModel);
        when(jenaService.getServiceCategories()).thenReturn(mockModel);
    }

    @Test
    void testMapToJenaModel() {
        var mockModel = mock(Model.class);
        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        var mockRes = mock(Resource.class);
        when(mockModel.getResource(anyString())).thenReturn(mockRes);
        var mockStm = mock(Statement.class);
        when(mockRes.getProperty(any())).thenReturn(mockStm);
        when(mockStm.getObject()).thenReturn(ResourceFactory.createPlainLiteral("test"));
        when(mockStm.getResource()).thenReturn(RDFS.Resource);

        UUID organizationId = UUID.randomUUID();

        //TODO: should we have 2 separate tests for ModelType.LIBRARY and ModelType.PROFILE?
        DataModelDTO dto = new DataModelDTO();
        dto.setPrefix("test");
        dto.setLabel(Map.of(
                "fi", "Test label fi",
                "sv", "Test label sv"));
        dto.setDescription(Map.of(
                "fi", "Test description fi",
                "sv", "Test description sv"));
        dto.setStatus(Status.DRAFT);
        dto.setGroups(Set.of("P11"));
        dto.setLanguages(Set.of("fi", "sv"));
        dto.setOrganizations(Set.of(organizationId));
        dto.setType(ModelType.PROFILE);
        dto.setInternalNamespaces(Set.of("http://uri.suomi.fi/datamodel/ns/newint"));
        var externalDTO = new ExternalNamespaceDTO();
        externalDTO.setName("test dto");
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");
        dto.setExternalNamespaces(Set.of(externalDTO));

        Model model = mapper.mapToJenaModel(dto);

        Resource modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource groupResource = model.getResource("http://urn.fi/URN:NBN:fi:uuid:au:ptvl:v1105");
        Resource organizationResource = model.getResource(String.format("urn:uuid:%s", organizationId));

        assertNotNull(modelResource);
        assertNotNull(groupResource);
        assertNotNull(organizationResource);

        assertEquals(2, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals(Status.DRAFT, Status.valueOf(modelResource.getProperty(OWL.versionInfo).getString()));

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals(1, modelResource.listProperties(DCTerms.requires).toList().size());
        assertNotNull(model.getResource("http://example.com/ns/ext"));
    }

    @Test
    void testMapToUpdateJenaModel() {
        Model m = ModelFactory.createDefaultModel();
        //TODO: should we have 2 separate tests for ModelType.LIBRARY and ModelType.PROFILE?
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        when(jenaService.getDataModel("test")).thenReturn(m);
        var mockModel = mock(Model.class);
        when(jenaService.getDataModel(anyString())).thenReturn(mockModel);
        var mockRes = mock(Resource.class);
        when(mockModel.getResource(anyString())).thenReturn(mockRes);
        var mockStm = mock(Statement.class);
        when(mockRes.getProperty(any())).thenReturn(mockStm);
        when(mockStm.getObject()).thenReturn(ResourceFactory.createPlainLiteral("test"));
        when(mockStm.getResource()).thenReturn(RDFS.Resource);

        UUID organizationId = UUID.randomUUID();

        DataModelDTO dto = new DataModelDTO();
        dto.setLabel(Map.of(
                "fi", "new test label"));
        dto.setDescription(Map.of(
                "fi", "new test description"));
        dto.setStatus(Status.DRAFT);
        dto.setGroups(Set.of("P11"));
        dto.setLanguages(Set.of("fi", "sv"));
        dto.setOrganizations(Set.of(organizationId));

        dto.setInternalNamespaces(Set.of("http://uri.suomi.fi/datamodel/ns/newint"));
        var externalDTO = new ExternalNamespaceDTO();
        externalDTO.setName("test dto");
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");

        dto.setExternalNamespaces(Set.of(externalDTO));

        //unchanged values
        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("testlabel", modelResource.listProperties(RDFS.label).next().getString());

        assertEquals(1, modelResource.listProperties(RDFS.comment).toList().size());
        assertEquals("test desc", modelResource.listProperties(RDFS.comment).next().getString());

        assertEquals(Status.VALID, Status.valueOf(modelResource.getProperty(OWL.versionInfo).getString()));

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int", modelResource.listProperties(OWL.imports).next().getString());

        assertEquals(1, modelResource.listProperties(DCTerms.requires).toList().size());
        assertEquals("https://www.example.com/ns/ext", modelResource.listProperties(DCTerms.requires).next().getString());

        Model model = mapper.mapToUpdateJenaModel("test", dto, m);

        //changed values
        modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource groupResource = model.getResource("http://urn.fi/URN:NBN:fi:uuid:au:ptvl:v1105");
        Resource organizationResource = model.getResource(String.format("urn:uuid:%s", organizationId));

        assertNotNull(modelResource);
        assertNotNull(groupResource);
        assertNotNull(organizationResource);

        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("new test label", modelResource.listProperties(RDFS.label, "fi").next().getString());

        assertEquals(Status.DRAFT, Status.valueOf(modelResource.getProperty(OWL.versionInfo).getString()));

        assertEquals(1, modelResource.listProperties(DCTerms.requires).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/newint", modelResource.listProperties(DCTerms.requires).next().getString());

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals("http://www.w3.org/2000/01/rdf-schema#", modelResource.listProperties(OWL.imports).next().getString());
    }

    @Test
    void testMapToDatamodelDTO() {
        Model m = ModelFactory.createDefaultModel();

        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var result = mapper.mapToDataModelDTO("test", m);

        assertEquals("test", result.getPrefix());
        assertEquals(ModelType.PROFILE, result.getType());
        assertEquals(Status.VALID, result.getStatus());

        assertEquals(1, result.getLabel().size());
        assertTrue(result.getLabel().containsValue("testlabel"));
        assertTrue(result.getLabel().containsKey("fi"));


        assertEquals(1, result.getDescription().size());
        assertTrue(result.getDescription().containsValue("test desc"));
        assertTrue(result.getDescription().containsKey("fi"));

        assertEquals(1, result.getLanguages().size());
        assertTrue(result.getLanguages().contains("fi"));

        assertEquals(1, result.getOrganizations().size());
        assertTrue(result.getOrganizations().contains(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));

        assertEquals(1, result.getGroups().size());
        assertTrue(result.getGroups().contains("P11"));
    }

    @Test
    void testMapToDatamodelDtoOld(){
        Model mOld = ModelFactory.createDefaultModel();
        var streamOld = getClass().getResourceAsStream("/test_datamodel_v1.ttl");
        assertNotNull(streamOld);
        RDFDataMgr.read(mOld, streamOld, RDFLanguages.TURTLE);
        var resultOld = mapper.mapToDataModelDTO("testaa", mOld);

        assertEquals("testaa", resultOld.getPrefix());
        assertEquals(ModelType.LIBRARY, resultOld.getType());
        assertEquals(Status.DRAFT, resultOld.getStatus());

        assertEquals(1, resultOld.getLabel().size());
        assertTrue(resultOld.getLabel().containsValue("Testaa"));
        assertTrue(resultOld.getLabel().containsKey("fi"));


        assertEquals(1, resultOld.getDescription().size());
        assertTrue(resultOld.getDescription().containsValue("Testaa desc"));
        assertTrue(resultOld.getDescription().containsKey("fi"));

        assertEquals(2, resultOld.getLanguages().size());
        assertTrue(resultOld.getLanguages().contains("fi"));
        assertTrue(resultOld.getLanguages().contains("en"));

        assertEquals(1, resultOld.getOrganizations().size());
        assertTrue(resultOld.getOrganizations().contains(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));

        assertEquals(1, resultOld.getGroups().size());
        assertTrue(resultOld.getGroups().contains("P11"));
    }

    @Test
    void testMapToIndexModel() {
        Model m = ModelFactory.createDefaultModel();

        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var result = mapper.mapToIndexModel("test", m);

        assertEquals("test", result.getPrefix());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test", result.getId());
        assertEquals(ModelType.PROFILE.name(), result.getType());
        assertEquals(Status.VALID.name(), result.getStatus());
        assertEquals("2023-01-03T12:44:45.799Z", result.getModified());
        assertEquals("2023-01-03T12:44:45.799Z", result.getCreated());
        assertEquals("2023-01-03T12:44:45.799Z", result.getContentModified());

        assertEquals(0, result.getDocumentation().size());

        assertEquals(1, result.getLabel().size());
        assertTrue(result.getLabel().containsValue("testlabel"));
        assertTrue(result.getLabel().containsKey("fi"));

        assertEquals(1, result.getComment().size());
        assertTrue(result.getComment().containsValue("test desc"));
        assertTrue(result.getComment().containsKey("fi"));

        assertEquals(1, result.getLanguage().size());
        assertTrue(result.getLanguage().contains("fi"));

        assertEquals(1, result.getContributor().size());
        assertTrue(result.getContributor().contains(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));

        assertEquals(1, result.getIsPartOf().size());
        assertTrue(result.getIsPartOf().contains("P11"));
    }

    @Test
    void testMapToIndexModelOld(){
        Model mOld = ModelFactory.createDefaultModel();

        var streamOld = getClass().getResourceAsStream("/test_datamodel_v1.ttl");
        assertNotNull(streamOld);
        RDFDataMgr.read(mOld, streamOld, RDFLanguages.TURTLE);

        var resultOld = mapper.mapToIndexModel("testaa", mOld);

        assertEquals("testaa", resultOld.getPrefix());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "testaa", resultOld.getId());
        assertEquals(ModelType.LIBRARY.name(), resultOld.getType());
        assertEquals(Status.DRAFT.name(), resultOld.getStatus());
        assertEquals("2018-03-20T16:21:07.067Z", resultOld.getModified());
        assertEquals("2018-03-20T17:59:44", resultOld.getCreated());

        assertEquals(0, resultOld.getDocumentation().size());

        assertEquals(1, resultOld.getLabel().size());
        assertTrue(resultOld.getLabel().containsValue("Testaa"));
        assertTrue(resultOld.getLabel().containsKey("fi"));

        assertEquals(1, resultOld.getComment().size());
        assertTrue(resultOld.getComment().containsValue("Testaa desc"));
        assertTrue(resultOld.getComment().containsKey("fi"));

        assertEquals(2, resultOld.getLanguage().size());
        assertTrue(resultOld.getLanguage().contains("fi"));
        assertTrue(resultOld.getLanguage().contains("en"));

        assertEquals(1, resultOld.getContributor().size());
        assertTrue(resultOld.getContributor().contains(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));

        assertEquals(1, resultOld.getIsPartOf().size());
        assertTrue(resultOld.getIsPartOf().contains("P11"));
    }



    @Test
    void testMapServiceCategoriesToDTO() {
        var model = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/service-categories.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, Lang.TURTLE);

        var serviceCategories = mapper.mapToListServiceCategoryDTO(model);

        assertEquals(3, serviceCategories.size());

        var cat = serviceCategories.get(0);

        assertEquals("P11", cat.getIdentifier());
        assertEquals("http://urn.fi/URN:NBN:fi:au:ptvl:v1105", cat.getId());
        assertEquals("Elinkeinot", cat.getLabel().get("fi"));
        assertEquals("Industries", cat.getLabel().get("en"));
        assertEquals("Näringar", cat.getLabel().get("sv"));
    }
}
