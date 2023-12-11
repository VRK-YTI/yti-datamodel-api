package fi.vm.yti.datamodel.api.mapper;


import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
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
        when(coreRepository.getServiceCategories()).thenReturn(MapperTestUtils.getMockGroups());
        when(coreRepository.getOrganizations()).thenReturn(MapperTestUtils.getMockOrganizations());
    }

    @ParameterizedTest
    @EnumSource(ModelType.class)
    void testMapToJenaModel(ModelType modelType) {
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource(ModelConstants.SUOMI_FI_NAMESPACE + "newint" + ModelConstants.RESOURCE_SEPARATOR)
                        .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile)
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
        dto.setInternalNamespaces(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "newint"));
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
        externalDTO.setName(Map.of("fi", "test dto"));
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");
        dto.setExternalNamespaces(Set.of(externalDTO));

        var linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", "new link"));
        linkDTO.setUri("https://example.com");
        linkDTO.setDescription(Map.of("fi", "link description"));
        dto.setLinks(Set.of(linkDTO));

        Model model = mapper.mapToJenaModel(dto, modelType, mockUser);

        Resource modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR);

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
                            ModelConstants.SUOMI_FI_NAMESPACE + "newint"
                    )));
        }

        assertNotNull(model.getResource("http://example.com/ns/ext"));

        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(modelResource, SuomiMeta.creator));
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(modelResource, SuomiMeta.modifier));

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
                """, MapperUtils.localizedPropertyToMap(modelResource, SuomiMeta.documentation).get("fi"));

        assertEquals("test@localhost", MapperUtils.propertyToString(modelResource, SuomiMeta.contact));

        var linkResource = modelResource.getProperty(RDFS.seeAlso);
        var linkObject = linkResource.getResource();
        assertEquals("new link", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.title).get("fi"));
        assertEquals("link description", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.description).get("fi"));
        assertEquals("https://example.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));
    }

    @Test
    void testMapToUpdateJenaModel() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var uri = DataModelURI.createModelURI("test");

        when(coreRepository.fetch("test")).thenReturn(m);
        var mockModel = ModelFactory.createDefaultModel();
        mockModel.createResource(ModelConstants.SUOMI_FI_NAMESPACE + "newint")
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

        dto.setInternalNamespaces(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "newint"));
        var externalDTO = new ExternalNamespaceDTO();
        externalDTO.setName(Map.of("fi", "test dto"));
        externalDTO.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        externalDTO.setPrefix("ext");

        dto.setExternalNamespaces(Set.of(externalDTO));

        var linkDTO = new LinkDTO();
        linkDTO.setName(Map.of("fi", "new link"));
        linkDTO.setUri("https://test.com");
        linkDTO.setDescription(Map.of("fi", "new link description"));
        dto.setLinks(Set.of(linkDTO));

        //unchanged values
        Resource modelResource = m.getResource(uri.getModelURI());
        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("testlabel", modelResource.listProperties(RDFS.label).next().getString());

        assertEquals(1, modelResource.listProperties(RDFS.comment).toList().size());
        assertEquals("test desc", modelResource.listProperties(RDFS.comment).next().getString());

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/", modelResource.listProperties(OWL.imports).next().getObject().toString());

        var requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        assertEquals(3, requires.size());
        assertTrue(requires.containsAll(List.of("https://www.example.com/ns/ext/", "http://uri.suomi.fi/terminology/test")));

        assertEquals("test@localhost", MapperUtils.propertyToString(modelResource, SuomiMeta.contact));

        assertEquals("""
                hello
                test""", MapperUtils.localizedPropertyToMap(modelResource, SuomiMeta.documentation).get("fi"));

        var linkResource = modelResource.getProperty(RDFS.seeAlso);
        var linkObject = linkResource.getResource();
        assertEquals("link title", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.title).get("fi"));
        assertEquals("link description", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.description).get("fi"));
        assertEquals("https://example.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));

        mapper.mapToUpdateJenaModel(uri.getModelURI(), dto, m, mockUser);

        //changed values
        modelResource = m.getResource(uri.getModelURI());

        assertEquals(1, modelResource.listProperties(RDFS.label).toList().size());
        assertEquals("new test label", modelResource.listProperties(RDFS.label, "fi").next().getString());

        requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        assertEquals(2, requires.size());
        assertTrue(requires.containsAll(List.of("http://www.w3.org/2000/01/rdf-schema#", "http://uri.suomi.fi/terminology/newtest")));

        assertEquals(1, modelResource.listProperties(OWL.imports).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "newint", modelResource.listProperties(OWL.imports).next().getObject().toString());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(modelResource, SuomiMeta.modifier));
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", MapperUtils.propertyToString(modelResource, SuomiMeta.creator));

        assertEquals("new@localhost", MapperUtils.propertyToString(modelResource, SuomiMeta.contact));
        assertEquals("""
                hello
                
                new test""", MapperUtils.localizedPropertyToMap(modelResource, SuomiMeta.documentation).get("fi"));

        linkResource = modelResource.getProperty(RDFS.seeAlso);
        linkObject = linkResource.getResource();
        assertEquals("new link", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.title).get("fi"));
        assertEquals("new link description", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.description).get("fi"));
        assertEquals("https://test.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));
    }

    @Test
    void testMapToUpdateModelProfile() {
        //Testing profile specific properties
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");
        var dto = new DataModelDTO();
        dto.setCodeLists(Set.of("http://uri.suomi.fi/codelist/test/newcodelist"));

        var uri = DataModelURI.createModelURI("test").getModelURI();

        var modelResource = m.getResource(uri);
        assertTrue(MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires)
                .contains("http://uri.suomi.fi/codelist/test/testcodelist"));

        mapper.mapToUpdateJenaModel(uri, dto, m, EndpointUtils.mockUser);

        //changed values
        modelResource = m.getResource(uri);
        assertTrue(MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires)
                .contains("http://uri.suomi.fi/codelist/test/newcodelist"));
    }

    @Test
    void testMapToDatamodelDTO() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var nsModel = MapperTestUtils.getModelFromFile("/test_datamodel_internal_reference.ttl");
        when(coreRepository.fetch(ModelConstants.SUOMI_FI_NAMESPACE + "int/")).thenReturn(nsModel);
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
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), result.getOrganizations().stream().findFirst().orElseThrow().getId());

        assertEquals(1, result.getGroups().size());
        assertEquals("P11", result.getGroups().stream().findFirst().orElseThrow().getIdentifier());

        assertEquals("test@localhost", result.getContact());

        assertEquals("""
                hello
                test""", result.getDocumentation().get("fi"));

        var internalDTO = result.getInternalNamespaces().stream().findFirst().orElseThrow();
        assertEquals("internal label", internalDTO.getName().get("fi"));
        assertEquals("int", internalDTO.getPrefix());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/", internalDTO.getNamespace());

        var externalDTO = result.getExternalNamespaces().stream()
                .filter(ns -> ns.getNamespace().equals("https://www.example.com/ns/ext/"))
                .findFirst()
                .orElseThrow();
        assertEquals("test resource", externalDTO.getName().get("fi"));
        assertEquals("extres", externalDTO.getPrefix());
        assertEquals("https://www.example.com/ns/ext/", externalDTO.getNamespace());

        assertEquals(1, result.getLinks().size());
        var linkDTO = result.getLinks().stream().findFirst().orElseThrow();
        assertEquals("link title", linkDTO.getName().get("fi"));
        assertEquals("link description", linkDTO.getDescription().get("fi"));
        assertEquals("https://example.com", linkDTO.getUri());
    }

    @Test
    void testMapToDatamodelDTOVersion() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_version.ttl");
        var nsModel = MapperTestUtils.getModelFromFile("/test_datamodel_internal_reference.ttl");
        when(coreRepository.fetch(ModelConstants.SUOMI_FI_NAMESPACE + "int")).thenReturn(nsModel);
        var result = mapper.mapToDataModelDTO("test", m, null);


        //no need to check other values since the file should be almost the same as above with the exception of version properties
        assertEquals("1.0.1", result.getVersion());
        assertEquals(Status.VALID, result.getStatus());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", result.getVersionIri());
    }

    @Test
    void testMapReleaseProperties() {
        //Model is loaded from the Draft version and mapping the properties will make it into a release version
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        final var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR;

        var uri = DataModelURI.createModelURI("test", "1.0.1");
        mapper.mapReleaseProperties(m, uri, Status.VALID);

        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", uri.getGraphURI());
        var resource = m.getResource(graphUri);
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", MapperUtils.propertyToString(resource, OWL2.versionIRI));

        assertEquals(Status.VALID, Status.valueOf(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("1.0.1", MapperUtils.propertyToString(resource, OWL.versionInfo));

        assertEquals(graphUri + "1.0.0/", MapperUtils.propertyToString(resource, OWL.priorVersion));
    }

    @Test
    void testMapPriorVersion() {
        //This is loaded as if it was a draft version
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        mapper.mapPriorVersion(m, ModelConstants.SUOMI_FI_NAMESPACE + "test", ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.0/");

        var resource = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test");
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.0/", MapperUtils.propertyToString(resource, OWL.priorVersion));
    }

    @Test
    void testMapToIndexModel() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test/";
        var result = mapper.mapToIndexModel(graphUri, m);

        assertEquals("test", result.getPrefix());
        assertEquals(graphUri, result.getId());
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
        assertTrue(result.getContributor().contains(MapperTestUtils.TEST_ORG_ID));

        assertEquals(1, result.getIsPartOf().size());
        assertTrue(result.getIsPartOf().contains("P11"));
    }

    @Test
    void mapModelVersionInfo() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_prior_versions.ttl");

        var uri = ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.0/";
        var result = mapper.mapModelVersionInfo(m.getResource(uri));
        assertEquals("Test", result.getLabel().get("fi"));
        assertEquals("1.0.0", result.getVersion());
        assertEquals(uri, result.getVersionIRI());
        assertEquals(Status.VALID, result.getStatus());
    }

    @Test
    void mapUpdateVersionedModel() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_version.ttl");
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR;
        var dto = new VersionedModelDTO();
        dto.setStatus(Status.RETIRED);
        dto.setDocumentation(Map.of("fi", "new documentation"));
        dto.setDescription(Map.of("fi", "new description"));
        dto.setLabel(Map.of("fi", "new label"));
        dto.setContact("new@localhost");
        dto.setGroups(Set.of("P17"));
        dto.setOrganizations(Set.of(UUID.fromString("74776e94-7f51-48dc-aeec-c084c4defa09")));
        var linkDTO = new LinkDTO();
        linkDTO.setDescription(Map.of("fi", "new link description"));
        linkDTO.setName(Map.of("fi", "new link"));
        linkDTO.setUri("https://test.com");
        dto.setLinks(Set.of(linkDTO));

        mapper.mapUpdateVersionedModel(m, graphUri, dto, EndpointUtils.mockUser);

        var resource = m.getResource(graphUri);
        assertEquals("new documentation", MapperUtils.localizedPropertyToMap(resource, SuomiMeta.documentation).get("fi"));
        assertEquals(Status.RETIRED, Status.valueOf(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("new description", MapperUtils.localizedPropertyToMap(resource, RDFS.comment).get("fi"));
        assertEquals("new label", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
        assertEquals("new@localhost", MapperUtils.propertyToString(resource, SuomiMeta.contact));
        assertEquals("http://urn.fi/URN:NBN:fi:au:ptvl:v1152", MapperUtils.propertyToString(resource, DCTerms.isPartOf));
        assertEquals("urn:uuid:74776e94-7f51-48dc-aeec-c084c4defa09", MapperUtils.propertyToString(resource, DCTerms.contributor));

        var linkResource = resource.getProperty(RDFS.seeAlso);
        var linkObject = linkResource.getResource();
        assertEquals("new link", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.title).get("fi"));
        assertEquals("new link description", MapperUtils.localizedPropertyToMap(linkObject, DCTerms.description).get("fi"));
        assertEquals("https://test.com", MapperUtils.propertyToString(linkObject, FOAF.homepage));

    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"VALID", "SUGGESTED", "RETIRED", "SUPERSEDED"}, mode = EnumSource.Mode.EXCLUDE)
    void mapUpdateVersionedModelInvalidStatuses(Status status) {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_version.ttl");
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR;
        var dto = new VersionedModelDTO();
        dto.setStatus(status);
        dto.setDocumentation(Map.of("fi", "new documentation"));
        var error = assertThrows(MappingError.class, () -> mapper.mapUpdateVersionedModel(m, graphUri, dto, EndpointUtils.mockUser));
        assertEquals("Error during mapping: Cannot change status from VALID to " + status.name(), error.getMessage());
    }

}
