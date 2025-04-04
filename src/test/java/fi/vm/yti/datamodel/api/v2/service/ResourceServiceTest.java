package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ResourceService.class
})
class ResourceServiceTest {

    @MockBean
    CoreRepository coreRepository;
    @MockBean
    ImportsRepository importsRepository;
    @MockBean
    DataModelAuthorizationManager authorizationManager;
    @MockBean
    GroupManagementService groupManagementService;
    @MockBean
    TerminologyService terminologyService;
    @MockBean
    CodeListService codeListService;
    @MockBean
    AuthenticatedUserProvider userProvider;
    @MockBean
    IndexService indexService;

    @Autowired
    ResourceService resourceService;

    private static final YtiUser USER = EndpointUtils.mockUser;
    private final Consumer<ResourceCommonInfoDTO> userMapper = (var dto) -> {};
    private final Consumer<ResourceInfoBaseDTO> conceptMapper = (var dto) -> {};
    @BeforeEach
    public void setup() {
        when(authorizationManager.hasRightToAnyOrganization(anyCollection())).thenReturn(true);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(USER);
        when(groupManagementService.mapUser()).thenReturn(userMapper);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);
    }

    @Test
    void get() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(OWL.Ontology));
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            resourceService.get("test", null, "TestResource");
        }
        var uri = DataModelURI.createResourceURI("test", "TestResource");

        verify(coreRepository).resourceExistsInGraph(uri.getGraphURI(), uri.getResourceURI());
        verify(coreRepository).fetch(uri.getGraphURI());
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(coreRepository).getOrganizations();

    }

    @Test
    void getWithVersion() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(OWL.Ontology));
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            resourceService.get("test", "1.0.1", "TestResource");
        }
        var dataModelURI = DataModelURI.createResourceURI("test", "TestResource", "1.0.1");

        verify(coreRepository).resourceExistsInGraph(dataModelURI.getGraphURI(), dataModelURI.getResourceURI());
        verify(coreRepository).fetch(dataModelURI.getGraphURI());
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(coreRepository).getOrganizations();

    }

    @ParameterizedTest
    @EnumSource(value = ResourceType.class, names = {"ATTRIBUTE", "ASSOCIATION"})
    void create(ResourceType resourceType) throws URISyntaxException {
        var model = EndpointUtils.getMockModel(OWL.Ontology);
        when(coreRepository.fetch(anyString())).thenReturn(model);

        var dataModelURI = DataModelURI.createResourceURI("test", "Identifier");
        var dto = createResourceDTO(false, resourceType);
        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToResource(any(DataModelURI.class), any(Model.class), any(ResourceDTO.class), any(ResourceType.class), any(YtiUser.class)))
                            .thenReturn(dataModelURI.getResourceURI());
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            var uri = resourceService.create(dataModelURI.getModelId(), dto, resourceType, false);
            assertEquals(dataModelURI.getResourceURI(), uri.toString());
        }

        verify(coreRepository).resourceExistsInGraph(anyString(), anyString(), eq(false));
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).createResourceToIndex(any(IndexResource.class));
    }

    @Test
    void createPropertyShape() throws URISyntaxException {
        var model = EndpointUtils.getMockModel(DCAP.DCAP);
        when(coreRepository.fetch(anyString())).thenReturn(model);

        var dataModelURI = DataModelURI.createResourceURI("test", "Identifier");
        var dto = createAssociationRestriction(false);
        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToPropertyShapeResource(any(DataModelURI.class), any(Model.class), any(PropertyShapeDTO.class), any(ResourceType.class), any(YtiUser.class)))
                    .thenReturn(dataModelURI.getResourceURI());
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            var uri = resourceService.create("test", dto, ResourceType.ASSOCIATION, true);
            assertEquals(dataModelURI.getResourceURI(), uri.toString());
        }

        verify(coreRepository).resourceExistsInGraph(anyString(), anyString(), eq(false));
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).createResourceToIndex(any(IndexResource.class));
    }

    @Test
    void updateResource() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(OWL.Ontology));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        var dto = createResourceDTO(true, ResourceType.ASSOCIATION);
        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            resourceService.update("test", "Identifier", dto);
            mapper.verify(() -> ResourceMapper.mapToUpdateResource(any(DataModelURI.class), any(Model.class), any(ResourceDTO.class), any(YtiUser.class)));
        }
        verify(coreRepository).resourceExistsInGraph(anyString(), anyString());
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).updateResourceToIndex(any(IndexResource.class));
    }

    @Test
    void updatePropertyShape() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(DCAP.DCAP));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        var dto = createAttributeRestriction(true);
        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            resourceService.update("test", "Identifier", dto);
            mapper.verify(() -> ResourceMapper.mapToUpdatePropertyShape(any(DataModelURI.class), any(Model.class), any(PropertyShapeDTO.class), any(YtiUser.class)));
        }
        verify(coreRepository).resourceExistsInGraph(anyString(), anyString());
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).updateResourceToIndex(any(IndexResource.class));
    }


    @Test
    void delete() {
        //Fail because doesnt exists
        assertThrows(ResourceNotFoundException.class, () -> resourceService.delete("test", "Identifier"));

        //delete
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        resourceService.delete("test", "identifier");

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).deleteResource(DataModelURI
                .createResourceURI("test", "identifier")
                .getResourceURI());
        verify(indexService).deleteResourceFromIndex(anyString());
    }


    @Test
    void copyPropertyShape() throws URISyntaxException {
        var sourceURI = DataModelURI.createResourceURI("test", "Identifier");
        var targetURI = DataModelURI.createResourceURI("newTest", "newId");
        when(coreRepository.resourceExistsInGraph(sourceURI.getGraphURI(), sourceURI.getResourceURI())).thenReturn(true);
        var m = ModelFactory.createDefaultModel();
        m.createResource(sourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile);
        m.createResource(targetURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile);
        when(coreRepository.fetch(anyString())).thenReturn(m);
        try(var mapper = mockStatic(ResourceMapper.class)) {
            mapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            var uri = resourceService.copyPropertyShape("test", "Identifier", "newTest", "newId");
            assertEquals(targetURI.getResourceURI(), uri.toString());
        }
        verify(coreRepository).resourceExistsInGraph(anyString(), anyString());
        verify(coreRepository).resourceExistsInGraph(anyString(), anyString(), eq(false));
        verify(coreRepository, times(2)).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
    }

    @Test
    void failCopyPropertyShapeNotExists() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        assertThrows(ResourceNotFoundException.class, () -> resourceService.copyPropertyShape("test", "Identifier", "newTest", "newId"));
    }

    @Test
    void failCopyPropertyShapeIdInUse() {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.resourceExistsInGraph(anyString(), anyString(), eq(false))).thenReturn(true);
        var error = assertThrows(MappingError.class, () -> resourceService.copyPropertyShape("test", "Identifier", "newTest", "newId"));
        assertEquals("Error during mapping: Identifier in use", error.getMessage());

    }

    @Test
    void failCopyPropertyShapeModelType() {
        var sourceURI = DataModelURI.createResourceURI("test", "Identifier");
        var targetURI = DataModelURI.createResourceURI("newTest", "newId");
        //Only one is application profile
        when(coreRepository.resourceExistsInGraph(sourceURI.getGraphURI(), sourceURI.getResourceURI())).thenReturn(true);
        var m = ModelFactory.createDefaultModel();
        m.createResource(sourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology);
        m.createResource(targetURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile);
        when(coreRepository.fetch(anyString())).thenReturn(m);
        var error = assertThrows(MappingError.class, () -> resourceService.copyPropertyShape("test", "Identifier", "newTest", "newId"));
        assertEquals("Error during mapping: Both data models have to be application profiles", error.getMessage());

        //Both are libraries
        m = ModelFactory.createDefaultModel();
        m.createResource(sourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology);
        m.createResource(targetURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology);
        when(coreRepository.fetch(anyString())).thenReturn(m);
        error = assertThrows(MappingError.class, () ->resourceService.copyPropertyShape("test", "Identifier", "newTest", "newId"));
        assertEquals("Error during mapping: Both data models have to be application profiles", error.getMessage());

    }

    @Test
    void checkExists(){
        var result = resourceService.exists("test", "corner-qwe");
        assertTrue(result);
        result = resourceService.exists("test", "Free");
        assertFalse(result);
        when(coreRepository.resourceExistsInGraph(anyString(), anyString(), eq(false))).thenReturn(true);
        result = resourceService.exists("test", "NotFree");
        assertTrue(result);
    }

    @Test
    void mapUriLabels() {
        var model = ModelFactory.createDefaultModel();
        var res = model.createResource("http://uri.suomi.fi/datamodel/ns/test_lib/attribute-1");
        MapperUtils.addLocalizedProperty(Set.of("en"), Map.of("en", "Class label"), res, RDFS.label);

        var uri1 = new UriDTO("http://uri.suomi.fi/datamodel/ns/test_lib/attribute-1", "test_lib:attribute-1", null);
        var uri2 = new UriDTO("http://uri.suomi.fi/datamodel/ns/test_lib/attribute-2", "test_lib:attribute-2", null);
        var uri3 = new UriDTO("http://uri.suomi.fi/datamodel/ns/test_lib/attribute-3", "test_lib:attribute-3", Map.of("en", "Existing label"));

        var uris = new HashSet<UriDTO>();
        uris.add(uri1);
        uris.add(uri2);
        uris.add(uri3);

        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(model);
        when(importsRepository.queryConstruct(any(Query.class))).thenReturn(ModelFactory.createDefaultModel());

        resourceService.mapUriLabels(Set.of()).accept(uris);

        var result1 = uris.stream()
                .filter(u -> u.getCurie().equals("test_lib:attribute-1"))
                .findFirst();
        var result2 = uris.stream()
                .filter(u -> u.getCurie().equals("test_lib:attribute-2"))
                .findFirst();
        var result3 = uris.stream()
                .filter(u -> u.getCurie().equals("test_lib:attribute-3"))
                .findFirst();

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertTrue(result3.isPresent());

        // map label from query result
        assertEquals(Map.of("en", "Class label"), result1.get().getLabel());
        // no label found -> use local name
        assertEquals(Map.of("en", "attribute-2"), result2.get().getLabel());
        // label already added
        assertEquals(Map.of("en", "Existing label"), result3.get().getLabel());
    }

    @Test
    void testRenameResource() throws URISyntaxException {
        var model = ModelFactory.createDefaultModel();
        var uri = DataModelURI.createResourceURI("test", "resource-1");
        model.createResource(uri.getResourceURI())
                .addProperty(DCTerms.identifier, "resource-1")
                .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(MapperUtils.getStatusUri(Status.DRAFT)))
                .addProperty(DCTerms.created, "created")
                .addProperty(DCTerms.modified, "modified");
        model.createResource(uri.getModelURI() + "resource-2")
                .addProperty(DCTerms.identifier, "resource-2")
                .addProperty(RDFS.subPropertyOf, ResourceFactory.createResource(uri.getResourceURI()));

        var newClassURI = uri.getModelURI() + "resource-1-new";

        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(coreRepository.fetch(uri.getGraphURI())).thenReturn(model);
        when(coreRepository.resourceExistsInGraph(uri.getGraphURI(), uri.getResourceURI())).thenReturn(true);
        when(coreRepository.resourceExistsInGraph(uri.getGraphURI(), newClassURI)).thenReturn(false);

        resourceService.renameResource("test", "resource-1", "resource-1-new");

        ArgumentCaptor<IndexResource> indexCaptor = ArgumentCaptor.forClass(IndexResource.class);
        verify(indexService).deleteResourceFromIndex(uri.getResourceURI());
        verify(indexService).createResourceToIndex(indexCaptor.capture());

        var renamed = model.getResource(newClassURI);

        assertEquals(newClassURI, renamed.getURI());
        assertEquals(newClassURI, model.getResource(uri.getModelURI() + "resource-2").getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals("resource-1-new", indexCaptor.getValue().getIdentifier());
    }

    @Test
    void testRenameResourceExists() {
        var uri = DataModelURI.createResourceURI("test", "resource-1");

        when(coreRepository.resourceExistsInGraph(uri.getGraphURI(), uri.getResourceURI())).thenReturn(true);
        when(coreRepository.resourceExistsInGraph(uri.getGraphURI(), uri.getModelURI() + "foo")).thenReturn(true);

        assertThrows(MappingError.class, () -> resourceService.renameResource("test", "resource-1", "foo"));
    }

    @Test
    void testGetResourceReferences() {
        var organizationId = USER.getOrganizations(Role.DATA_MODEL_EDITOR).iterator().next();
        var qs = mock(QuerySolution.class);

        when(qs.get("resource")).thenReturn(ResourceFactory.createResource("https://iri.suomi.fi/model/ref/test-ref"));
        when(qs.contains("version")).thenReturn(true);
        when(qs.get("version")).thenReturn(ResourceFactory.createTypedLiteral("1.0.0"));
        when(qs.get("contributor")).thenReturn(ResourceFactory.createTypedLiteral(organizationId));
        when(qs.get("label")).thenReturn(ResourceFactory.createLangLiteral("test label", "fi"));
        when(qs.get("type")).thenReturn(OWL.Class);
        when(qs.get("predicate")).thenReturn(RDFS.subClassOf);

        doAnswer(ans -> {
            Consumer<QuerySolution> cons = ans.getArgument(1, Consumer.class);
            cons.accept(qs);
            return null;
        }).when(coreRepository).querySelect(any(Query.class), any(Consumer.class));

        var captor = ArgumentCaptor.forClass(Model.class);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            resourceService.getResourceReferences("https://iri.suomi.fi/model/ref/test");
            mapper.verify(() -> ResourceMapper.mapToResourceReference(captor.capture()));
        }

        var model = captor.getValue();
        var resource = model.getResource("https://iri.suomi.fi/model/ref/1.0.0/test-ref");

        assertTrue(resource.listProperties().hasNext());
        assertEquals(OWL.Class, resource.getProperty(RDF.type).getObject());
        assertEquals("test label", resource.getProperty(RDFS.label).getString());
        assertEquals(RDFS.subClassOf, resource.getProperty(DCTerms.references).getObject());
    }

    @Test
    void testMapDraftResourceReferences() {
        var qs = mock(QuerySolution.class);

        // no version found (=draft) and user has no permissions
        when(qs.get("res")).thenReturn(ResourceFactory.createResource("https://iri.suomi.fi/model/ref/child"));
        when(qs.get("contributor")).thenReturn(ResourceFactory.createTypedLiteral(UUID.randomUUID().toString()));

        doAnswer(ans -> {
            Consumer<QuerySolution> cons = ans.getArgument(1, Consumer.class);
            cons.accept(qs);
            return null;
        }).when(coreRepository).querySelect(any(Query.class), any(Consumer.class));

        var captor = ArgumentCaptor.forClass(Model.class);

        try(var mapper = mockStatic(ResourceMapper.class)) {
            resourceService.getResourceReferences("https://iri.suomi.fi/model/ref/parent");
            mapper.verify(() -> ResourceMapper.mapToResourceReference(captor.capture()));
        }

        assertTrue(captor.getValue().isEmpty());
    }

    private static ResourceDTO createResourceDTO(boolean update, ResourceType resourceType){
        var dto = new ResourceDTO();
        dto.setEditorialNote("test comment");
        if(!update){
            dto.setIdentifier("Identifier");
        }
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/FakeResource"));
        dto.setSubResourceOf(Set.of("http://uri.suomi.fi/datamodel/ns/int/FakeResource"));
        dto.setDomain("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        if(resourceType.equals(ResourceType.ASSOCIATION)){
            dto.setRange("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        }else{
            dto.setRange("owl:real");
        }
        dto.setNote(Map.of("fi", "test note"));
        return dto;
    }

    private static AttributeRestriction createAttributeRestriction(boolean update) {
        var dto = new AttributeRestriction();
        dto.setLabel(Map.of("fi", "test label"));
        if(!update) {
            dto.setIdentifier("Identifier");
        }
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setPath("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        return dto;
    }

    private static AssociationRestriction createAssociationRestriction(boolean update) {
        var dto = new AssociationRestriction();
        dto.setLabel(Map.of("fi", "test label"));
        if(!update) {
            dto.setIdentifier("Identifier");
        }
        dto.setSubject("sanastot.suomi.fi/notrealurl");
        dto.setPath("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        dto.setClassType("http://uri.suomi.fi/datamodel/ns/int/FakeClass");
        return dto;
    }


}
