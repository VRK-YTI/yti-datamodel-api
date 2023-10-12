package fi.vm.yti.datamodel.api.mapper;


import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import({
        ModelMapper.class
})
class ModelMapperTest {

    @MockBean
    CoreRepository coreRepository;
    @MockBean
    DataModelService dataModelService;
    @MockBean
    ConceptRepository conceptRepository;
    @MockBean
    ImportsRepository importsRepository;
    @MockBean
    SchemesRepository schemesRepository;
    @Autowired
    ModelMapper mapper;

    @BeforeEach
    public void init(){
        var mockGroups = ModelFactory.createDefaultModel();
        mockGroups.createResource("http://urn.fi/URN:NBN:fi:au:ptvl:v1105")
                .addProperty(SKOS.notation, "P11")
                .addProperty(RDFS.label, ResourceFactory.createLangLiteral("test group", "fi"));
        when(coreRepository.getServiceCategories()).thenReturn(mockGroups);

        var mockOrgs = ModelFactory.createDefaultModel();
        mockOrgs.createResource("urn:uuid:7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")
                .addProperty(RDF.type, FOAF.Organization)
                .addProperty(SKOS.prefLabel, ResourceFactory.createLangLiteral("test org", "fi"));
        when(coreRepository.getOrganizations()).thenReturn(mockOrgs);
    }

    @ParameterizedTest
    @EnumSource(ModelType.class)
    void testMapToJenaModel(ModelType modelType) {
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource("http://uri.suomi.fi/datamodel/ns/newint")
                        .addProperty(RDF.type, OWL.Ontology)
                        .addProperty(RDF.type, Iow.ApplicationProfile)
                        .addProperty(DCAP.preferredXMLNamespacePrefix, "test");
        when(coreRepository.fetch(anyString())).thenReturn(mockModel);

        UUID organizationId = UUID.randomUUID();
        YtiUser mockUser = EndpointUtils.mockUser;

        DataModelDTO dto = new DataModelDTO();
        dto.setPrefix("test");
        dto.setLabel(Map.of(
                "fi", "Test label fi",
                "sv", "Test label sv"));
        dto.setDescription(Map.of(
                "fi", "Test description fi",
                "sv", "Test description sv"));
        dto.setGroups(Set.of("P11"));
        dto.setLanguages(Set.of("fi", "sv"));
        dto.setOrganizations(Set.of(organizationId));
        dto.setInternalNamespaces(Set.of("http://uri.suomi.fi/datamodel/ns/newint"));
        dto.setContact("test@localhost");
        if (modelType.equals(ModelType.PROFILE)) {
            dto.setCodeLists(Set.of("http://uri.suomi.fi/codelist/test/testcodelist"));
        }
        dto.setTerminologies(Set.of("http://uri.suomi.fi/terminology/test"));
        dto.setDocumentation(Map.of("fi","""
                test documentation
                # Header
                **bold**
                """));
        var externalDTO = new ExternalNamespaceDTO();
        externalDTO.setName("test dto");
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");
        dto.setExternalNamespaces(Set.of(externalDTO));

        var linkDTO = new LinkDTO();
        linkDTO.setName("new link");
        linkDTO.setUri("https://example.com");
        linkDTO.setDescription("link description");
        dto.setLinks(Set.of(linkDTO));

        Model model = mapper.mapToJenaModel(dto, modelType, mockUser);

        Resource modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource groupResource = model.getResource("http://urn.fi/URN:NBN:fi:uuid:au:ptvl:v1105");
        Resource organizationResource = model.getResource(String.format("urn:uuid:%s", organizationId));

        assertNotNull(modelResource);
        assertNotNull(groupResource);
        assertNotNull(organizationResource);

        assertEquals(2, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals(Status.DRAFT, Status.valueOf(modelResource.getProperty(SuomiMeta.publicationStatus).getString()));


        var requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        assertEquals(3, requires.size());
        
        if(modelType.equals(ModelType.PROFILE)){
            assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
            assertTrue(requires.containsAll(
                    List.of("http://www.w3.org/2000/01/rdf-schema#",
                            "http://uri.suomi.fi/terminology/test",
                            "http://uri.suomi.fi/codelist/test/testcodelist"
                    )));
        }else{
            assertEquals(0, modelResource.listProperties(OWL.imports).toList().size());
            assertTrue(requires.containsAll(
                    List.of("http://www.w3.org/2000/01/rdf-schema#",
                            "http://uri.suomi.fi/terminology/test",
                            "http://uri.suomi.fi/datamodel/ns/newint"
                    )));
        }

        assertNotNull(model.getResource("http://example.com/ns/ext"));

        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(modelResource, Iow.creator));
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(modelResource, Iow.modifier));

        if (modelType.equals(ModelType.PROFILE)) {
            assertTrue(MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires).contains("http://uri.suomi.fi/codelist/test/testcodelist"));
        } else {
            assertFalse(MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires).contains("http://uri.suomi.fi/codelist/test/testcodelist"));
        }
        assertTrue(MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires).contains("http://uri.suomi.fi/terminology/test"));

        assertEquals("""
                test documentation
                # Header
                **bold**
                """, MapperUtils.localizedPropertyToMap(modelResource, Iow.documentation).get("fi"));

        assertEquals("test@localhost", MapperUtils.propertyToString(modelResource, Iow.contact));

        var linkResource = modelResource.getProperty(RDFS.seeAlso);
        var linkObject = linkResource.getResource();
        assertEquals("new link", MapperUtils.propertyToString(linkObject, DCTerms.title));
        assertEquals("link description", MapperUtils.propertyToString(linkObject, DCTerms.description));
        assertEquals("https://example.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));
    }

    @Test
    void testMapToUpdateJenaModel() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        when(coreRepository.fetch("test")).thenReturn(m);
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource("http://uri.suomi.fi/datamodel/ns/newint")
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(DCAP.preferredXMLNamespacePrefix, "test");
        when(coreRepository.fetch(anyString())).thenReturn(mockModel);

        UUID organizationId = UUID.randomUUID();
        YtiUser mockUser = EndpointUtils.mockUser;

        DataModelDTO dto = new DataModelDTO();
        dto.setLabel(Map.of(
                "fi", "new test label"));
        dto.setDescription(Map.of(
                "fi", "new test description"));
        dto.setGroups(Set.of("P11"));
        dto.setLanguages(Set.of("fi", "sv"));
        dto.setOrganizations(Set.of(organizationId));
        dto.setContact("new@localhost");
        dto.setTerminologies(Set.of("http://uri.suomi.fi/terminology/newtest"));
        dto.setDocumentation(Map.of("fi", """
                hello
                
                new test"""));

        dto.setInternalNamespaces(Set.of("http://uri.suomi.fi/datamodel/ns/newint"));
        var externalDTO = new ExternalNamespaceDTO();
        externalDTO.setName("test dto");
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");

        dto.setExternalNamespaces(Set.of(externalDTO));

        var linkDTO = new LinkDTO();
        linkDTO.setName("new link");
        linkDTO.setUri("https://test.com");
        linkDTO.setDescription("new link description");
        dto.setLinks(Set.of(linkDTO));

        //unchanged values
        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("testlabel", modelResource.listProperties(RDFS.label).next().getString());

        assertEquals(1, modelResource.listProperties(RDFS.comment).toList().size());
        assertEquals("test desc", modelResource.listProperties(RDFS.comment).next().getString());

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int", modelResource.listProperties(OWL.imports).next().getObject().toString());

        var requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        assertEquals(2, requires.size());
        assertTrue(requires.containsAll(List.of("https://www.example.com/ns/ext", "http://uri.suomi.fi/terminology/test")));

        assertEquals("test@localhost", MapperUtils.propertyToString(modelResource, Iow.contact));

        assertEquals("""
                hello
                test""", MapperUtils.localizedPropertyToMap(modelResource, Iow.documentation).get("fi"));

        var linkResource = modelResource.getProperty(RDFS.seeAlso);
        var linkObject = linkResource.getResource();
        assertEquals("link title", MapperUtils.propertyToString(linkObject, DCTerms.title));
        assertEquals("link description", MapperUtils.propertyToString(linkObject, DCTerms.description));
        assertEquals("https://example.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));

        Model model = mapper.mapToUpdateJenaModel("test", dto, m, mockUser);

        //changed values
        modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource groupResource = model.getResource("http://urn.fi/URN:NBN:fi:uuid:au:ptvl:v1105");
        Resource organizationResource = model.getResource(String.format("urn:uuid:%s", organizationId));

        assertNotNull(modelResource);
        assertNotNull(groupResource);
        assertNotNull(organizationResource);

        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("new test label", modelResource.listProperties(RDFS.label, "fi").next().getString());

        requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        assertEquals(2, requires.size());
        assertTrue(requires.containsAll(List.of("http://www.w3.org/2000/01/rdf-schema#", "http://uri.suomi.fi/terminology/newtest")));

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/newint", modelResource.listProperties(OWL.imports).next().getObject().toString());
        assertEquals(mockUser.getId().toString(), modelResource.getProperty(Iow.modifier).getString());
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", modelResource.getProperty(Iow.creator).getObject().toString());

        assertEquals("new@localhost", MapperUtils.propertyToString(modelResource, Iow.contact));
        assertEquals("""
                hello
                
                new test""", MapperUtils.localizedPropertyToMap(modelResource, Iow.documentation).get("fi"));

        linkResource = modelResource.getProperty(RDFS.seeAlso);
        linkObject = linkResource.getResource();
        assertEquals("new link", MapperUtils.propertyToString(linkObject, DCTerms.title));
        assertEquals("new link description", MapperUtils.propertyToString(linkObject, DCTerms.description));
        assertEquals("https://test.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));
    }

    @Test
    void testMapToUpdateModelProfile(){
        //Testing profile specific properties
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");
        when(coreRepository.fetch("test")).thenReturn(m);
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource("http://uri.suomi.fi/datamodel/ns/newint")
                .addProperty(RDF.type, Iow.ApplicationProfile)
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(DCAP.preferredXMLNamespacePrefix, "test");
        when(coreRepository.fetch(anyString())).thenReturn(mockModel);

        YtiUser mockUser = EndpointUtils.mockUser;

        DataModelDTO dto = new DataModelDTO();
        dto.setCodeLists(Set.of("http://uri.suomi.fi/codelist/test/newcodelist"));

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        assertEquals("http://uri.suomi.fi/codelist/test/testcodelist", MapperUtils.propertyToString(modelResource, DCTerms.requires));

        Model model = mapper.mapToUpdateJenaModel("test", dto, m, mockUser);


        //changed values
        modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        assertEquals("http://uri.suomi.fi/codelist/test/newcodelist", MapperUtils.propertyToString(modelResource, DCTerms.requires));


    }

    @Test
    void testMapToDatamodelDTO() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var nsModel = MapperTestUtils.getModelFromFile("/test_datamodel_internal_reference.ttl");
        when(coreRepository.fetch("http://uri.suomi.fi/datamodel/ns/int")).thenReturn(nsModel);
        var result = mapper.mapToDataModelDTO("test", m, null);

        assertEquals("test", result.getPrefix());
        assertEquals(ModelType.LIBRARY, result.getType());
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
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", result.getOrganizations().stream().findFirst().orElseThrow().getId());

        assertEquals(1, result.getGroups().size());
        assertEquals("P11", result.getGroups().stream().findFirst().orElseThrow().getIdentifier());

        assertEquals("test@localhost", result.getContact());

        assertEquals("""
                hello
                test""", result.getDocumentation().get("fi"));

        var internalDTO = result.getInternalNamespaces().stream().findFirst().orElseThrow();
        assertEquals("internal label", internalDTO.getName().get("fi"));
        assertEquals("int", internalDTO.getPrefix());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int", internalDTO.getNamespace());

        var externalDTO = result.getExternalNamespaces().stream().findFirst().orElseThrow();
        assertEquals("test resource", externalDTO.getName());
        assertEquals("extres", externalDTO.getPrefix());
        assertEquals("https://www.example.com/ns/ext", externalDTO.getNamespace());

        assertEquals(1, result.getLinks().size());
        var linkDTO = result.getLinks().stream().findFirst().orElseThrow();
        assertEquals("link title", linkDTO.getName());
        assertEquals("link description", linkDTO.getDescription());
        assertEquals("https://example.com", linkDTO.getUri());
    }

    @Test
    void testMapToDatamodelDTOVersion() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_version.ttl");
        var nsModel = MapperTestUtils.getModelFromFile("/test_datamodel_internal_reference.ttl");
        when(coreRepository.fetch("http://uri.suomi.fi/datamodel/ns/int")).thenReturn(nsModel);
        var result = mapper.mapToDataModelDTO("test", m, null);


        //no need to check other values since the file should be almost the same as above with the exception of version properties
        assertEquals("1.0.1", result.getVersion());
        assertEquals(Status.VALID, result.getStatus());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/1.0.1", result.getVersionIri());
    }

    @Test
    void testMapReleaseProperties() {
        //Model is loaded from the Draft version and mapping the properties will make it into a release version
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        final var graphUri = "http://uri.suomi.fi/datamodel/ns/test";

        var versionUri = mapper.mapReleaseProperties(m, graphUri, "1.0.1", Status.VALID);

        assertEquals("http://uri.suomi.fi/datamodel/ns/test/1.0.1", versionUri);
        var resource = m.getResource(graphUri);
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/1.0.1", MapperUtils.propertyToString(resource, OWL2.versionIRI));

        assertEquals(Status.VALID, Status.valueOf(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("1.0.1", MapperUtils.propertyToString(resource, OWL.versionInfo));

        assertEquals(graphUri + "/1.0.0", MapperUtils.propertyToString(resource, OWL.priorVersion));
    }

    @Test
    void testMapPriorVersion() {
        //This is loaded as if it was a draft version
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        mapper.mapPriorVersion(m, "http://uri.suomi.fi/datamodel/ns/test", "http://uri.suomi.fi/datamodel/ns/test/1.0.0");

        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/1.0.0", MapperUtils.propertyToString(resource, OWL.priorVersion));
    }

    @Test
    void testMapToIndexModel() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var result = mapper.mapToIndexModel("http://uri.suomi.fi/datamodel/ns/test", m);

        assertEquals("test", result.getPrefix());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test", result.getId());
        assertEquals(ModelType.LIBRARY, result.getType());
        assertEquals(Status.VALID, result.getStatus());
        assertEquals("2023-01-03T12:44:45.799Z", result.getModified());
        assertEquals("2023-01-03T12:44:45.799Z", result.getCreated());
        assertEquals("2023-01-03T12:44:45.799Z", result.getContentModified());

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
    void mapModelVersionInfo() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_prior_versions.ttl");


        var result = mapper.mapModelVersionInfo(m.getResource("http://uri.suomi.fi/datamodel/ns/test/1.0.0"));
        assertEquals("Test", result.getLabel().get("fi"));
        assertEquals("1.0.0", result.getVersion());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/1.0.0", result.getVersionIRI());
        assertEquals(Status.VALID, result.getStatus());
    }
}
