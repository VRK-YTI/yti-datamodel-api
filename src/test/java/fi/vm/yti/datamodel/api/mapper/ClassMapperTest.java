package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
class ClassMapperTest {

    @Test
    void testCreateClassAndMapToModelLibrary() {
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var mockUser = EndpointUtils.mockUser;

        ClassDTO dto = new ClassDTO();
        dto.setIdentifier("TestClass");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentClass(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqClass"));
        dto.setSubClassOf(Set.of("https://www.example.com/ns/ext/SubClass"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));

        ClassMapper.createOntologyClassAndMapToModel(uri, m, dto, mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource classResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals(uri.getResourceURI(), modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(1, classResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", classResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", classResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(OWL.Class, classResource.getProperty(RDF.type).getResource());
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(classResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), classResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, classResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("TestClass", classResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", classResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", classResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, classResource.listProperties(RDFS.subClassOf).toList().size());
        assertEquals("https://www.example.com/ns/ext/SubClass", classResource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(1, classResource.listProperties(OWL.equivalentClass).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqClass", classResource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(classResource, SuomiMeta.creator));
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(classResource, SuomiMeta.modifier));
    }

    @Test
    void testCreateClassAndMapToModelLibraryOwlThing() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var mockUser = EndpointUtils.mockUser;

        ClassDTO dto = new ClassDTO();
        dto.setSubClassOf(Collections.emptySet());
        dto.setIdentifier("Identifier");

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ClassMapper.createOntologyClassAndMapToModel(uri, m, dto, mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource classResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, classResource.listProperties(RDFS.subClassOf).toList().size());
        assertEquals(OWL.Thing, classResource.getProperty(RDFS.subClassOf).getResource());
    }

    @Test
    void testMapToClassDTOLibrary(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var dto = ClassMapper.mapToClassDTO(m, DataModelURI.createResourceURI("test", "TestClass"), MapperTestUtils.getMockOrganizations(), false, null);

        // not authenticated
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals("TestClass", dto.getIdentifier());
        assertEquals(1, dto.getEquivalentClass().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqClass"), dto.getEquivalentClass().stream().findFirst().orElse(null));
        assertEquals(1, dto.getSubClassOf().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubClass"), dto.getSubClassOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getLabel().size());
        assertEquals("test label", dto.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestClass", dto.getUri());
    }

    @Test
    void testMapToClassMinimalDTO(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var dto = ClassMapper.mapToClassDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null);

        // not authenticated
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals("TestClass", dto.getIdentifier());
        assertTrue(dto.getEquivalentClass().isEmpty());
        assertTrue(dto.getSubClassOf().isEmpty());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test label", dto.getLabel().get("fi"));
        assertNull(dto.getSubject());
        assertTrue(dto.getNote().isEmpty());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(uri.getResourceURI(), dto.getUri());
    }

    @Test
    void testMapToClassDTOAuthenticatedUser() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        Consumer<ResourceCommonDTO> userMapper = (var dto) -> {
            var creator = new UserDTO("123");
            var modifier = new UserDTO("123");
            creator.setName("creator fake-user");
            modifier.setName("modifier fake-user");
            dto.setCreator(creator);
            dto.setModifier(modifier);
        };

        var dto = ClassMapper.mapToClassDTO(m, DataModelURI.createResourceURI("test", "TestClass"), MapperTestUtils.getMockOrganizations(), true, userMapper);

        assertEquals("comment visible for admin", dto.getEditorialNote());
        assertEquals("creator fake-user", dto.getCreator().getName());
        assertEquals("modifier fake-user", dto.getModifier().getName());
    }

    @Test
    void testMapToUpdateClassLibrary(){
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource(uri.getResourceURI());
        var mockUser = EndpointUtils.mockUser;

        var dto = new ClassDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentClass(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq"));
        dto.setSubClassOf(Set.of("https://www.example.com/ns/ext/NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqClass", resource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .next().getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());

        ClassMapper.mapToUpdateOntologyClass(m, uri.getModelURI(), resource, dto, mockUser);

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq", resource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .next().getObject().toString());
        assertEquals("https://www.example.com/ns/ext/NewSub", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(resource, SuomiMeta.modifier));
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", MapperUtils.propertyToString(resource, SuomiMeta.creator));

        // class restrictions added to owl:equivalentClass property should remain
        assertEquals(1, resource.listProperties(OWL.equivalentClass).filterKeep(p -> p.getObject().isAnon()).toList().size());
    }

    // null values should delete (some) properties from resource
    @Test
    void testMapToUpdateClassNullValuesDTO(){
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource(uri.getResourceURI());
        var mockUser = EndpointUtils.mockUser;

        var dto = new ClassDTO();

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqClass", resource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .next().getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());

        ClassMapper.mapToUpdateOntologyClass(m, uri.getModelURI(), resource, dto, mockUser);

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertNull(resource.getProperty(RDFS.label));
        assertNull(resource.getProperty(RDFS.label));
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertNull(resource.getProperty(DCTerms.subject));
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertNull(resource.getProperty(SKOS.editorialNote));
        assertEquals(0, resource.listProperties(RDFS.comment).toList().size());
    }

    @Test
    void testMapToUpdateClassEmptyValuesDTO(){
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource(uri.getResourceURI());
        var mockUser = EndpointUtils.mockUser;

        var dto = new ClassDTO();
        dto.setSubject("");
        dto.setEquivalentClass(Collections.emptySet());
        dto.setSubClassOf(Collections.emptySet());
        dto.setEditorialNote("");
        dto.setNote(Collections.emptyMap());

        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqClass", resource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .next().getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());

        ClassMapper.mapToUpdateOntologyClass(m, uri.getModelURI(), resource, dto, mockUser);

        assertNull(resource.getProperty(DCTerms.subject));
        assertEquals(0, resource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .toList().size());
        //OWl thing is default value if all subClassOf is emptied
        assertEquals(OWL.Thing, resource.getProperty(RDFS.subClassOf).getResource());
        assertNull(resource.getProperty(SKOS.editorialNote));
        assertNull(resource.getProperty(RDFS.comment));
    }

    @Test
    void testAddAttributeAndAssociationRestrictionsToClassDTO(){
        var m = MapperTestUtils.getModelFromFile("/models/test_resource_query_model.ttl");

        var uri = DataModelURI.createResourceURI("test", "rangetest2");
        var restriction = new SimpleResourceDTO();
        restriction.setUri(uri.getResourceURI());
        restriction.setRange(new UriDTO("http://www.w3.org/2001/XMLSchema#integer", "xsd:integer"));

        var dto = new ClassInfoDTO();
        ClassMapper.addClassResourcesToDTO(m, Set.of(restriction), dto, (var simpleResourceDTO) -> {});

        assertEquals(1, dto.getAttribute().size());

        var attribute = dto.getAttribute().get(0);

        assertEquals("test label", attribute.getLabel().get("fi"));
        assertEquals("test", attribute.getIdentifier());
        assertEquals(uri.getResourceURI(), attribute.getUri());
        assertEquals("test", attribute.getModelId());
        assertEquals("1.0.0", attribute.getVersion());
        assertEquals("xsd:integer", attribute.getRange().getCurie());
    }

    @Test
    void testMapExternalClass() {
        var m = MapperTestUtils.getModelFromFile("/external_class.ttl");

        String ns = "http://purl.org/ontology/mo/";
        var dto = ClassMapper.mapExternalClassToDTO(m, ns + "AudioFile");

        assertEquals("audio file", dto.getLabel().get("en"));
        assertEquals(ns + "AudioFile", dto.getUri());

        var attributes = dto.getAttributes();
        assertEquals(1, attributes.size());
        assertEquals("encoding", attributes.get(0).getLabel().get("en"));
        assertEquals(ns + "encoding", attributes.get(0).getUri());
    }

    @Test
    void testMapAndRemoveAnonymousRestrictions() {

        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");

        var classResource1 = model.getResource(DataModelURI.createResourceURI("model", "class-1").getResourceURI());
        var classResource2 = model.getResource(DataModelURI.createResourceURI("model", "class-2").getResourceURI());
        var attributeResource1 = model.getResource(DataModelURI.createResourceURI("model", "attribute-1").getResourceURI());
        var attributeResource2 = model.getResource(DataModelURI.createResourceURI("model", "attribute-2").getResourceURI());

        ClassMapper.mapClassRestrictionProperty(model, classResource2, attributeResource1);

        Consumer<Resource> checkRestriction = (var res) -> {
            var eqClassResource = getEqResource(res);
            assertNotNull(eqClassResource);
            var restrictionResources = eqClassResource.getProperty(OWL.intersectionOf).getList().asJavaList();

            assertEquals(OWL.Class, eqClassResource.getProperty(RDF.type).getResource());
            assertTrue(restrictionResources.stream().allMatch(r ->
                    r.asResource().hasProperty(RDF.type) &&
                            r.asResource().hasProperty(OWL.onProperty) &&
                            r.asResource().hasProperty(OWL.someValuesFrom)));
            assertTrue(restrictionResources.stream()
                    .allMatch(r -> r.asResource().getProperty(RDF.type).getObject().equals(OWL.Restriction) &&
                            r.asResource().getProperty(OWL.someValuesFrom).getObject().equals(XSD.anyURI)));
        };

        List.of(classResource1, classResource2).forEach(checkRestriction);

        var classEqResource1 = getEqResource(classResource1);
        assertNotNull(classEqResource1);
        var attributeURIs1 = classEqResource1.getProperty(OWL.intersectionOf).getList().asJavaList()
                .stream().map(r -> r.asResource().getProperty(OWL.onProperty).getObject().toString())
                .toList();
        assertEquals(List.of(attributeResource1.getURI(), attributeResource2.getURI()), attributeURIs1);

        var classEqResource2 = getEqResource(classResource2);
        assertNotNull(classEqResource2);
        var attributeURIs2 = classEqResource2.getProperty(OWL.intersectionOf).getList().asJavaList()
                .stream().map(r -> r.asResource().getProperty(OWL.onProperty).getObject().toString())
                .toList();
        assertEquals(List.of(attributeResource1.getURI()), attributeURIs2);

        // remove all restriction references
        ClassMapper.mapRemoveClassRestrictionProperty(model, classResource1, attributeResource1, null);
        ClassMapper.mapRemoveClassRestrictionProperty(model, classResource1, attributeResource2, null);
        ClassMapper.mapRemoveClassRestrictionProperty(model, classResource2, attributeResource1, null);

        assertNull(getEqResource(classResource1));
        assertNull(getEqResource(classResource2));
        assertNotNull(classResource1.getProperty(OWL.equivalentClass));
    }

    @Test
    void testMapUpdateClassRestriction() {
        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");

        var uri = DataModelURI.createResourceURI("model", "class-update-target");
        var classResource = model.getResource(uri.getResourceURI());
        var restrictionURI = DataModelURI.createResourceURI("model", "association-1").getResourceURI();
        var oldTarget = DataModelURI.createResourceURI("model", "class-2").getResourceURI();
        var newTarget = DataModelURI.createResourceURI("model", "class-x").getResourceURI();

        ClassMapper.mapUpdateClassRestrictionProperty(model, classResource, restrictionURI, oldTarget,
                newTarget, ResourceType.ASSOCIATION);

        var eqResource = getEqResource(classResource);
        assertNotNull(eqResource);
        var restrictions = eqResource.getProperty(OWL.intersectionOf).getList().asJavaList().stream()
                .filter(r -> r.asResource().getProperty(OWL.onProperty).getObject().toString().equals(restrictionURI))
                .toList();

        assertEquals(2, restrictions.size());
        assertEquals(newTarget, restrictions.get(0).asResource().getProperty(OWL.someValuesFrom).getObject().toString());
    }

    @Test
    void testMapUpdateClassRestrictionDuplicate() {
        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");

        var uri = DataModelURI.createResourceURI("model", "class-update-target");
        var classResource = model.getResource(uri.getResourceURI());
        var restrictionURI = DataModelURI.createResourceURI("model", "association-1").getResourceURI();
        var oldTarget = DataModelURI.createResourceURI("model", "class-2").getResourceURI();
        var newTargetDuplicate = DataModelURI.createResourceURI("model", "class-3").getResourceURI();

        assertThrows(MappingError.class, () ->
                ClassMapper.mapUpdateClassRestrictionProperty(model, classResource, restrictionURI,
                        oldTarget, newTargetDuplicate, ResourceType.ASSOCIATION));
    }

    @Test
    void testMapReferenceResourceAndAddNamespace() {
        var model = ModelFactory.createDefaultModel();
        var uri = DataModelURI.createResourceURI("test", "class-1");

        model.createResource(uri.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name());

        var dto = new ClassDTO();
        dto.setIdentifier(uri.getResourceId());
        dto.setSubClassOf(Set.of(
                ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/sub",
                uri.getModelURI() + "test-sub"
        ));
        dto.setEquivalentClass(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/eq"));
        dto.setDisjointWith(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-2/disjoint"));

        ClassMapper.createOntologyClassAndMapToModel(uri, model, dto, EndpointUtils.mockUser);

        var modelResource = model.getResource(uri.getModelURI());

        var imports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        assertEquals(2, imports.size());
        assertTrue(imports.containsAll(Set.of(
                ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/",
                ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-2/"
                ))
        );
    }

    @Test
    void testMapTerminologyToModel() {
        var model = ModelFactory.createDefaultModel();
        var uri = DataModelURI.createModelURI("test");

        model.createResource(uri.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name());

        var classDTO = new ClassDTO();
        classDTO.setIdentifier("class-1");
        classDTO.setSubject(ModelConstants.TERMINOLOGY_NAMESPACE + "test-terminology/concept-1");

        ClassMapper.createOntologyClassAndMapToModel(DataModelURI.createModelURI("test"), model, classDTO, EndpointUtils.mockUser);

        var requires = model.getResource(uri.getModelURI())
                .listProperties(DCTerms.requires)
                .mapWith(s -> s.getObject().toString())
                .toList();

        assertTrue(requires.contains(ModelConstants.TERMINOLOGY_NAMESPACE + "test-terminology"));
    }

    private Resource getEqResource(Resource res) {
        var eq = res.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getResource().isAnon())
                .mapWith(p -> p.getObject().asResource())
                .toList();

        if (eq.isEmpty()) {
            return null;
        } else {
            return eq.get(0);
        }
    }

}
