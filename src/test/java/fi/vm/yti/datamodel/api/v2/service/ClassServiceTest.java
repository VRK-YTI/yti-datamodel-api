package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.dto.NodeShapeDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceInfoBaseDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ClassService.class
})
class ClassServiceTest {

    @MockBean
    CoreRepository coreRepository;

    @MockBean
    DataModelAuthorizationManager authorizationManager;

    @MockBean
    ImportsRepository importsRepository;

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    TerminologyService terminologyService;

    @MockBean
    GroupManagementService groupManagementService;

    @MockBean
    IndexService indexService;

    @MockBean
    SearchIndexService searchIndexService;

    @MockBean
    VisualizationService visualizationService;

    @SpyBean
    @Autowired
    ClassService classService;

    Consumer<ResourceInfoBaseDTO> conceptMapper = (resource) -> {};

    @Test
    void getLibraryClass() {
        var model = ModelFactory.createDefaultModel();

        var resourceURI = DataModelURI.Factory.createResourceURI("test", "TestClass");
        model.createResource(resourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));

        var resource = model.createResource(resourceURI.getResourceURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));
        MapperUtils.addCreationMetadata(resource, EndpointUtils.mockUser);

        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);
        var searchResult = new SearchResponseDTO<IndexResource>();
        searchResult.setResponseObjects(List.of());
        when(searchIndexService.findResourcesByURI(anySet(), eq(null))).thenReturn(searchResult);

        classService.get(resourceURI.getModelId(), null, resourceURI.getResourceId());

        var graphURI = Constants.DATA_MODEL_NAMESPACE + "test" + Constants.RESOURCE_SEPARATOR;
        verify(coreRepository).resourceExistsInGraph(graphURI, Constants.DATA_MODEL_NAMESPACE + "test/TestClass");
        verify(coreRepository).fetch(graphURI);
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).getOrganizations();
    }

    @Test
    void getWithVersion() {
        var model = ModelFactory.createDefaultModel();

        var resourceURI = DataModelURI.Factory.createResourceURI("test", "TestClass", "1.0.1");
        model.createResource(resourceURI.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));

        var resource = model.createResource(resourceURI.getResourceURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));
        MapperUtils.addCreationMetadata(resource, EndpointUtils.mockUser);

        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);

        var searchResult = new SearchResponseDTO<IndexResource>();
        searchResult.setResponseObjects(List.of());
        when(searchIndexService.findResourcesByURI(anySet(), anyString())).thenReturn(searchResult);
        when(searchIndexService.findResourcesByURI(anySet(), eq(null))).thenReturn(searchResult);

        classService.get(resourceURI.getModelId(), resourceURI.getVersion(), resourceURI.getResourceId());

        verify(coreRepository).resourceExistsInGraph(Constants.DATA_MODEL_NAMESPACE + "test/1.0.1/", Constants.DATA_MODEL_NAMESPACE + "test/TestClass");
        verify(coreRepository).fetch(Constants.DATA_MODEL_NAMESPACE + "test/1.0.1/");
        verify(authorizationManager).hasRightToModel(eq("test"), any(Model.class));
        verify(coreRepository).getOrganizations();
    }

    @Test
    void createClass() throws URISyntaxException {
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        var dto = createClassDTO(false);
        try(var mapper = mockStatic(ClassMapper.class);
            var resMapper = mockStatic(ResourceMapper.class)) {
            resMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            var uri = classService.create("test", dto,false);
            assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/Identifier", uri.toString());
            mapper.verify(() -> ClassMapper.createOntologyClassAndMapToModel(any(DataModelURI.class), any(Model.class), any(ClassDTO.class), any(YtiUser.class)));
        }

        verify(coreRepository).resourceExistsInGraph(anyString(), anyString(), eq(false));
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).createResourceToIndex(any(IndexResource.class));
        verify(visualizationService).addNewResourceDefaultPosition("test","Identifier");
    }

    @Test
    void createNodeShape() throws URISyntaxException {
        var resourceURI = DataModelURI.Factory.createResourceURI("test", "node-shape-1");
        var nodeRefURI = DataModelURI.Factory.createResourceURI("foo", "bar", "1.2.3");

        var model = ModelFactory.createDefaultModel();
        model.createResource(resourceURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT))
                .addProperty(DCTerms.requires, ResourceFactory.createResource(nodeRefURI.getGraphURI()));

        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);

        // mock fetching properties based on sh:node reference
        var nodeReferenceResult = ModelFactory.createDefaultModel();
        nodeReferenceResult.createResource(nodeRefURI.getResourceURI())
                .addProperty(SH.property, ResourceFactory.createResource(nodeRefURI.getModelURI() + "node-property"));
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(nodeReferenceResult);

        var dto = createNodeShapeDTO(false);
        dto.setTargetNode(nodeRefURI.getResourceVersionURI());

        var uri = classService.create("test", dto,true);

        verify(coreRepository).resourceExistsInGraph(anyString(), anyString(), eq(false));
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).createResourceToIndex(any(IndexResource.class));

        var result = model.getResource(uri.toString());
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/node-shape-1", uri.toString());

        var expectedPropertyURI = DataModelURI.Factory.createResourceURI(nodeRefURI.getModelId(), "node-property", nodeRefURI.getVersion());
        assertEquals(expectedPropertyURI.getResourceVersionURI(), result.listProperties(SH.property).toList().get(0).getObject().toString());
        assertEquals(nodeRefURI.getResourceVersionURI(), result.getProperty(SH.node).getObject().toString());
    }

    @Test
    void createNodeShapeWithTargetClassProperties() throws URISyntaxException {
        var model = ModelFactory.createDefaultModel();
        var profilePrefix = "test-profile";
        var libPrefix = "test";

        var nodeShapeURI = DataModelURI.Factory.createResourceURI(profilePrefix, "node-shape-1");
        var targetClassURI = DataModelURI.Factory.createResourceURI(libPrefix, "class-1", "1.0.0");

        var modelResource = model.createResource(nodeShapeURI.getModelURI())
                .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT))
                .addProperty(DCAP.preferredXMLNamespacePrefix, profilePrefix)
                .addProperty(DCTerms.language, "fi");
        var targetClass = ResourceFactory.createResource(targetClassURI.getResourceVersionURI());

        var targetProperty1 = DataModelURI.Factory.createResourceURI(libPrefix, "attribute-1", "1.0.0");
        var targetProperty2 = DataModelURI.Factory.createResourceURI(libPrefix, "other-property", "1.0.0");

        // create resource with sh:path reference to library's attribute-1 resource
        // new property based on test:attribute-1 should not be created
        model.createResource(DataModelURI.Factory.createResourceURI(profilePrefix, "attribute-1").getResourceURI())
                .addProperty(SH.path, ResourceFactory.createResource(targetProperty1.getResourceVersionURI()));

        var dto = new NodeShapeDTO();
        dto.setIdentifier(nodeShapeURI.getResourceId());
        dto.setTargetClass(targetClass.getURI());
        dto.setProperties(Set.of(targetProperty1.getResourceVersionURI(), targetProperty2.getResourceVersionURI()));

        when(coreRepository.fetch(modelResource.getURI())).thenReturn(model);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);

        // mock fetching target class info
        var libModel = EndpointUtils.getMockModel(OWL.Ontology);
        var searchResult = new SearchResponseDTO<IndexResource>();
        searchResult.setResponseObjects(List.of());
        libModel.createResource(targetClassURI.getResourceURI())
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name())
                .addProperty(DCTerms.created, "created")
                .addProperty(DCTerms.modified, "modified");
        when(coreRepository.resourceExistsInGraph(targetClass.getNameSpace(), targetClassURI.getResourceURI())).thenReturn(true);
        when(coreRepository.fetch(targetClass.getNameSpace())).thenReturn(libModel);
        when(coreRepository.getOrganizations()).thenReturn(ModelFactory.createDefaultModel());
        when(searchIndexService.findResourcesByURI(new HashSet<>(), null)).thenReturn(searchResult);
        when(terminologyService.mapConcept()).thenReturn(conceptMapper);

        var propertyResult = new SearchResponseDTO<IndexResource>();
        var indexResource = new IndexResource();
        indexResource.setId(targetProperty2.getResourceVersionURI());
        indexResource.setIdentifier(targetProperty2.getResourceId());
        indexResource.setResourceType(ResourceType.ATTRIBUTE);
        indexResource.setLabel(Map.of("fi", "other target resource"));
        propertyResult.setResponseObjects(List.of(indexResource));

        // should fetch the property not yet added to the application profile
        when(searchIndexService.findResourcesByURI(Set.of(targetProperty2.getResourceVersionURI()),null))
                .thenReturn(propertyResult);

        classService.create(profilePrefix, dto, true);

        // node shape properties should contain both existing and new property
        var nodeShapeResult = model.getResource(nodeShapeURI.getResourceURI());
        var nodeShapeProperties = MapperUtils.arrayPropertyToList(nodeShapeResult, SH.property);
        assertEquals(2, nodeShapeProperties.size());
        assertTrue(List.of(
                DataModelURI.Factory.createResourceURI(profilePrefix, "other-property").getResourceURI(),
                DataModelURI.Factory.createResourceURI(profilePrefix, "attribute-1").getResourceURI()
            ).containsAll(nodeShapeProperties));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<IndexResource>> indexCaptor = ArgumentCaptor.forClass(List.class);

        // should index non-existing new property (other-property)
        verify(indexService).bulkInsert(anyString(), indexCaptor.capture());

        var indexedProperties = indexCaptor.getValue();
        assertEquals(1, indexedProperties.size());
        assertEquals(DataModelURI.Factory.createResourceURI(
                profilePrefix, "other-property").getResourceURI(), indexedProperties.get(0).getId());
    }

    @Test
    void updateClass() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(OWL.Ontology));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        var dto = createClassDTO(true);
        try(var mapper = mockStatic(ClassMapper.class);
            var resMapper = mockStatic(ResourceMapper.class)) {
            resMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classService.update("test", "Identifier", dto);
            mapper.verify(() -> ClassMapper.mapToUpdateOntologyClass( any(Model.class), anyString(),any(Resource.class), any(ClassDTO.class), any(YtiUser.class)));
        }
        verify(coreRepository).resourceExistsInGraph(anyString(), anyString());
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(terminologyService).resolveConcept(anyString());
        verify(coreRepository).put(anyString(), any(Model.class));
        verify(indexService).updateResourceToIndex(any(IndexResource.class));
    }

    @Test
    void updateNodeShape() {
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(EndpointUtils.getMockModel(DCAP.DCAP));
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(EndpointUtils.mockUser);
        var dto = createNodeShapeDTO(true);
        try(var mapper = mockStatic(ClassMapper.class);
            var resMapper = mockStatic(ResourceMapper.class)) {
            resMapper.when(() -> ResourceMapper.mapToIndexResource(any(Model.class), anyString())).thenReturn(new IndexResource());
            classService.update("test", "Identifier", dto);
            mapper.verify(() -> ClassMapper.mapToUpdateNodeShape( any(Model.class), anyString(),any(Resource.class), any(NodeShapeDTO.class), anySet(), any(YtiUser.class)));
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
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        classService.delete("test", "Identifier");

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).deleteResource(DataModelURI.Factory
                .createResourceURI("test", "Identifier")
                .getResourceURI());
        verify(indexService).deleteResourceFromIndex(anyString());
    }

    @Test
    void deleteNotExists() {
        assertThrows(ResourceNotFoundException.class, () -> classService.delete("test", "Identifier"));
    }

    @Test
    void exists() {
        var response = classService.exists("test", "Identifier");
         assertFalse(response);

        //reserved
        response = classService.exists("test", "corner-wqe");
        assertTrue(response);

        //exists
        when(coreRepository.resourceExistsInGraph(anyString(), anyString(), eq(false))).thenReturn(true);
        response = classService.exists("test", "Identifier");
        assertTrue(response);
    }

    @Test
    void handlePropertyShapeReferencePut(){
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        try(var mapper = mockStatic(ClassMapper.class)) {
            classService.handlePropertyShapeReference("test", "node-shape-1", Constants.DATA_MODEL_NAMESPACE + "test/ref-1", false);
            mapper.verify(() -> ClassMapper.mapAppendNodeShapeProperty( any(Resource.class), anyString(), anySet()));
        }

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).put(anyString(), any(Model.class));
    }

    @Test
    void handlePropertyShapeReferenceDelete(){
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        try(var mapper = mockStatic(ClassMapper.class)) {
            classService.handlePropertyShapeReference("test", "node-shape-1", Constants.DATA_MODEL_NAMESPACE + "test/ref-1", true);
            mapper.verify(() -> ClassMapper.mapRemoveNodeShapeProperty(any(Model.class), any(Resource.class), anyString(), anySet()));
        }

        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).put(anyString(), any(Model.class));
    }

    @Test
    void togglePropertyShape(){
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        try(var mapper = mockStatic(ClassMapper.class)) {
            classService.togglePropertyShape("test", Constants.DATA_MODEL_NAMESPACE + "test/Uri");
            mapper.verify(() -> ClassMapper.toggleAndMapDeactivatedProperty( any(Model.class), anyString(), anyBoolean()));
        }

        verify(coreRepository).resourceExistsInGraph(anyString(), anyString());
        verify(coreRepository).fetch(anyString());
        verify(authorizationManager).hasRightToModel(anyString(), any(Model.class));
        verify(coreRepository).put(anyString(), any(Model.class));
    }

    @Test
    void testAddClassRestriction() {
        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");
        var modelURI = Constants.DATA_MODEL_NAMESPACE + "model" + Constants.RESOURCE_SEPARATOR;

        String attributeURI = modelURI + Constants.RESOURCE_SEPARATOR + "attribute-1";

        var restrictionQueryResult = ModelFactory.createDefaultModel();
        restrictionQueryResult.createResource(attributeURI)
                .addProperty(RDF.type, OWL.DatatypeProperty)
                .addProperty(RDFS.range, XSD.integer);

        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(restrictionQueryResult);
        when(importsRepository.queryConstruct(any(Query.class))).thenReturn(ModelFactory.createDefaultModel());

        classService.handleAddClassRestrictionReference("model", "class-2",
                attributeURI);

        var captor = ArgumentCaptor.forClass(Model.class);
        verify(coreRepository).put(eq(modelURI), captor.capture());

        // updated model contains with attributeURI as an object
        var restrictionProperty = captor.getValue().listStatements(null, OWL.onProperty,
                ResourceFactory.createResource(attributeURI));
        var targetProperty = captor.getValue().listStatements(null, OWL.someValuesFrom, XSD.integer);

        assertTrue(restrictionProperty.hasNext());
        assertTrue(targetProperty.hasNext());
    }

    @Test
    void testUpdateClassRestrictionTarget() {
        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");
        var modelURI = Constants.DATA_MODEL_NAMESPACE + "model" + Constants.RESOURCE_SEPARATOR;

        var restrictionQueryResult = ModelFactory.createDefaultModel();
        var restrictionResource = restrictionQueryResult.createResource(modelURI + "association-1")
                .addProperty(RDF.type, OWL.ObjectProperty);

        var restrictionNewTargetResult = ModelFactory.createDefaultModel();
        restrictionNewTargetResult.createResource(modelURI + "some-class")
                .addProperty(RDF.type, OWL.Class);

        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);
        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(coreRepository.resourceExistsInGraph(anyString(), anyString())).thenReturn(true);
        when(classService.findResources(eq(Set.of(modelURI + "association-1")), anySet())).thenReturn(restrictionQueryResult);
        when(classService.findResources(eq(Set.of(modelURI + "some-class")), anySet())).thenReturn(restrictionNewTargetResult);

        classService.handleUpdateClassRestrictionReference("model", "class-update-target",
                restrictionResource.getURI(),
                modelURI + "class-2",
                modelURI + "some-class");

        var captor = ArgumentCaptor.forClass(Model.class);
        verify(coreRepository).put(eq(modelURI), captor.capture());

        // updated model contains triple with property OWL.someValuesFrom and "some-class" as an object
        var stmtIterator = captor.getValue().listStatements(null, OWL.someValuesFrom,
                ResourceFactory.createResource(modelURI + "some-class"));

        assertTrue(stmtIterator.hasNext());
    }

    @Test
    void testCheckCyclicalReferences() {
        classService.checkCyclicalReference(Constants.DATA_MODEL_NAMESPACE + "Model-2/Class-1", OWL.equivalentClass, Constants.DATA_MODEL_NAMESPACE + "Model-1/class-1");

        var captor = ArgumentCaptor.forClass(Query.class);
        verify(coreRepository).queryAsk(captor.capture());
        assertEquals("""
                        ASK
                        WHERE
                          { <https://iri.suomi.fi/model/Model-2/Class-1> (<http://www.w3.org/2002/07/owl#equivalentClass>){*} <https://iri.suomi.fi/model/Model-1/class-1>}
                        """,
                captor.getValue().toString());
    }

    @Test
    void testAddAndRemoveCodeListToLibraryAttribute() {
        var classURI = DataModelURI.Factory.createResourceURI("test", "TestClass");
        var attributeURI = DataModelURI.Factory.createResourceURI("test", "TestAttribute");
        var codeListURI = "http://uri.suomi.fi/codelist/test-code-list";

        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        when(coreRepository.fetch(classURI.getGraphURI())).thenReturn(model);
        when(authorizationManager.hasRightToModel(eq("test"), any(Model.class))).thenReturn(true);
        when(coreRepository.resourceExistsInGraph(classURI.getGraphURI(), classURI.getResourceURI()))
                .thenReturn(true);

        // add code list
        classService.addCodeList("test", "TestClass", attributeURI.getResourceURI(), Set.of(codeListURI));

        ClassMapper.getClassRestrictionList(model, model.getResource(classURI.getResourceURI()))
                .stream()
                .filter(r -> attributeURI.getResourceURI().equals(MapperUtils.propertyToString(r, OWL.onProperty)))
                .findFirst()
                .ifPresentOrElse(
                        r -> {
                            assertEquals(codeListURI, MapperUtils.propertyToString(r, SuomiMeta.codeList));
                            assertEquals(XSD.anyURI, r.getProperty(OWL.someValuesFrom).getObject());
                        },
                        Assertions::fail);

        // remove code list
        classService.removeCodeList("test", "TestClass", attributeURI.getResourceURI(), codeListURI);
        assertEquals(0, model.listSubjectsWithProperty(SuomiMeta.codeList).toList().size());
    }


    private static ClassDTO createClassDTO(boolean update){
        var dto = new ClassDTO();
        dto.setEditorialNote("test comment");
        if(!update){
            dto.setIdentifier("Identifier");
        }
        dto.setSubject("http://uri.suomi.fi/terminology/notrealurl");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setEquivalentClass(Set.of(Constants.DATA_MODEL_NAMESPACE + "notrealns/FakeClass"));
        dto.setSubClassOf(Set.of(Constants.DATA_MODEL_NAMESPACE + "notrealns/FakeClass"));
        dto.setNote(Map.of("fi", "test note"));
        return dto;
    }

    private static NodeShapeDTO createNodeShapeDTO(boolean update) {
        var dto = new NodeShapeDTO();
        dto.setLabel(Map.of("fi", "node label"));
        if(!update){
            dto.setIdentifier("node-shape-1");
        }
        dto.setProperties(Set.of());
        dto.setSubject("http://uri.suomi.fi/terminology/concept-123");

        return dto;
    }

}
