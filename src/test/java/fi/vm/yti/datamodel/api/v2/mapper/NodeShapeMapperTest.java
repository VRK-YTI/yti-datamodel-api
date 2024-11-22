package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.HTTP;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class NodeShapeMapperTest {

    @Test
    void testCreateNodeShapeAndMapToProfile() {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");

        var mockUser = EndpointUtils.mockUser;

        var property1 = new IndexResource();
        property1.setId(DataModelURI.createResourceURI(
                "test_lib", "association-1", "1.0.0").getResourceVersionURI());
        property1.setIdentifier("association-1");
        property1.setResourceType(ResourceType.ASSOCIATION);
        property1.setLabel(Map.of(
                "fi", "Association association-1",
                "en", "Association EN")
        );

        var property2 = new IndexResource();
        property2.setId(DataModelURI.createResourceURI(
                "test_lib", "attribute-1", "1.0.0").getResourceVersionURI());
        property2.setIdentifier("attribute-1");
        property2.setResourceType(ResourceType.ATTRIBUTE);
        property2.setLabel(Map.of("fi", "Attribute attribute-1"));

        // assume there is a resource with identifier association-1 defined in the model
        // suffix -1 should be added when creating new property shape resource
        Predicate<String> freePrefixCheck = s -> s.equals(Constants.DATA_MODEL_NAMESPACE + "test/association-1");

        var dto = new NodeShapeDTO();
        dto.setIdentifier("TestClass");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));
        dto.setTargetClass(Constants.DATA_MODEL_NAMESPACE + "target/Class");
        dto.setApiPath("/api/path/");

        var attributeRestriction = new SimpleResourceDTO();
        attributeRestriction.setUri(DataModelURI
                .createResourceURI("test_lib","attribute-1", "1.0.0")
                .getResourceVersionURI());
        attributeRestriction.setCodeLists(Set.of("http://uri.suomi.fi/codelist/test"));

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        ClassMapper.createNodeShapeAndMapToModel(uri, model, dto, mockUser);
        ClassMapper.mapPlaceholderPropertyShapes(model, uri.getResourceURI(),
                List.of(property1, property2), mockUser, freePrefixCheck, List.of(attributeRestriction));

        Resource modelResource = model.getResource(uri.getModelURI());
        Resource classResource = model.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, classResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", classResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", classResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(SH.NodeShape, classResource.getProperty(RDF.type).getResource());
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(classResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), classResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, classResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("TestClass", classResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        assertEquals("test note", classResource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(classResource, SuomiMeta.creator));
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(classResource, SuomiMeta.modifier));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "target/Class", classResource.getProperty(SH.targetClass).getObject().toString());

        checkPropertyShapes(classResource, model);
    }

    private static void checkPropertyShapes(Resource classResource, Model model) {
        assertEquals(2, classResource.listProperties(SH.property).toList().size());
        var propertyShapeAttribute = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/attribute-1");
        var propertyShapeAssociation = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/association-1-1");

        assertNotNull(propertyShapeAttribute);
        assertNotNull(propertyShapeAssociation);

        // PropertyShapes should have two type properties
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, OWL.DatatypeProperty));

        assertTrue(MapperUtils.hasType(propertyShapeAssociation, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAssociation, OWL.ObjectProperty));

        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test_lib/1.0.0/attribute-1",
                propertyShapeAttribute.getProperty(SH.path).getObject().toString());
        assertEquals(Status.DRAFT, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(propertyShapeAttribute, SuomiMeta.publicationStatus)));
        assertEquals("Attribute attribute-1", MapperUtils.localizedPropertyToMap(propertyShapeAttribute, RDFS.label).get("fi"));
        assertEquals("Association EN", MapperUtils.localizedPropertyToMap(propertyShapeAssociation, RDFS.label).get("en"));
        assertEquals("/api/path/", MapperUtils.propertyToString(classResource, HTTP.API_PATH));
        assertEquals(Set.of("http://uri.suomi.fi/codelist/test"), MapperUtils.arrayPropertyToSet(propertyShapeAttribute, SuomiMeta.codeList));
    }

    @Test
    void testMapNodeShapeToClassDTO(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var dto = ClassMapper.mapToNodeShapeDTO(m, uri, MapperTestUtils.getMockOrganizations(), false, null);

        // not authenticated
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals("TestClass", dto.getIdentifier());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test label", dto.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals(2, dto.getNote().size());
        assertEquals("test technical description fi", dto.getNote().get("fi"));
        assertEquals("test technical description en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/TestClass", dto.getUri());
        assertEquals(new UriDTO(Constants.DATA_MODEL_NAMESPACE + "target/Class"), dto.getTargetClass());
        assertEquals("/api/path/", dto.getApiPath());
    }

    @Test
    void testMapReferencePropertyShapes() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        ClassMapper.mapNodeShapeProperties(model, Constants.DATA_MODEL_NAMESPACE + "test/TestClass",
                Set.of(Constants.DATA_MODEL_NAMESPACE + "test_lib/attribute-1", Constants.DATA_MODEL_NAMESPACE + "test_lib/association-1")
        );

        var classRes = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");

        var propertyShapes = classRes.listProperties(SH.property).mapWith(p -> p.getObject().toString()).toList();

        // should contain both existing in the model and those found from with query
        assertEquals(4, propertyShapes.size());
        assertTrue(propertyShapes.containsAll(List.of(
                Constants.DATA_MODEL_NAMESPACE + "test_lib/attribute-1",
                Constants.DATA_MODEL_NAMESPACE + "test_lib/association-1",
                Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction",
                Constants.DATA_MODEL_NAMESPACE + "test/DeactivatedPropertyShape"))
        );
    }

    @Test
    void testMapToUpdateClassProfile(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var resource = m.getResource(uri.getResourceURI());
        var mockUser = EndpointUtils.mockUser;

        var dto = new NodeShapeDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setNote(Map.of("fi", "new note"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setTargetClass(Constants.DATA_MODEL_NAMESPACE + "target/NewClass");

        assertEquals(SH.NodeShape, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "target/Class", resource.getProperty(SH.targetClass).getObject().toString());

        ClassMapper.mapToUpdateNodeShape(m, uri.getModelURI(), resource, dto, Set.of(), mockUser);

        assertEquals(SH.NodeShape, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(resource, SuomiMeta.modifier));
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", MapperUtils.propertyToString(resource, SuomiMeta.creator));
    }

    @Test
    void testMapNodeShapeResources() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var deactivatedURI = DataModelURI.createResourceURI("test", "DeactivatedPropertyShape");
        var resource = model.getResource(uri.getResourceURI());
        var dto = new NodeShapeInfoDTO();
        dto.setUri(resource.getURI());

        var extResourceURI = DataModelURI.createResourceURI("test_profile", "ps-1", "1.0.0");
        var externalPropertyShape = new IndexResource();
        externalPropertyShape.setResourceType(ResourceType.ATTRIBUTE);
        externalPropertyShape.setIdentifier(extResourceURI.getResourceId());
        externalPropertyShape.setUri(extResourceURI.getResourceVersionURI());
        externalPropertyShape.setFromVersion(extResourceURI.getVersion());
        externalPropertyShape.setLabel(Map.of("fi", "Test Ext"));

        ClassMapper.addCurrentModelNodeShapeResources(model, resource, dto);
        ClassMapper.addExternalNodeShapeResource(List.of(externalPropertyShape), dto);
        ClassMapper.updateNodeShapeResourceRestrictions(model, dto.getAttribute(), Set.of(deactivatedURI.getResourceURI()));

        var attributes = dto.getAttribute();
        assertEquals(3, attributes.size());

        // deactivated property shape
        var result1 = attributes.stream()
                .filter(SimplePropertyShapeDTO::isDeactivated).findFirst();
        if (result1.isPresent()) {
            var deactivated = result1.get();
            assertEquals("DeactivatedPropertyShape", deactivated.getIdentifier());
            assertEquals("test", deactivated.getModelId());
            assertEquals("deactivated property shape", deactivated.getLabel().get("fi"));
            assertTrue(deactivated.isDeactivated());
        } else {
            fail("No deactivated property shape found");
        }

        // external property shape
        var result2 = attributes.stream().filter(a -> a.getModelId().equals("test_profile")).findFirst();
        if (result2.isPresent()) {
            var ext = result2.get();
            assertEquals("ps-1", ext.getIdentifier());
            assertEquals("test_profile", ext.getModelId());
            assertEquals("1.0.0", ext.getVersion());
            assertFalse(ext.isDeactivated());
        } else {
            fail("No external property shape found");
        }
    }

    @Test
    void testMapDeactivatedPropertyShape() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var propertyShape1 = m.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction");
        var propertyShape2 = m.getResource(Constants.DATA_MODEL_NAMESPACE + "test/DeactivatedPropertyShape");

        assertFalse(propertyShape1.hasProperty(SH.deactivated));
        assertTrue(propertyShape2.getProperty(SH.deactivated).getObject().asLiteral().getBoolean());

        ClassMapper.toggleAndMapDeactivatedProperty(m, Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction", false);
        ClassMapper.toggleAndMapDeactivatedProperty(m, Constants.DATA_MODEL_NAMESPACE + "test/DeactivatedPropertyShape", false);

        assertTrue(propertyShape1.getProperty(SH.deactivated).getObject().asLiteral().getBoolean());
        assertFalse(propertyShape2.hasProperty(SH.deactivated));
    }

    @Test
    void testAppendNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");
        var propertyURI = Constants.DATA_MODEL_NAMESPACE + "foo";

        assertEquals(2, classResource.listProperties(SH.property).toList().size());

        ClassMapper.mapAppendNodeShapeProperty(classResource, propertyURI, Set.of());

        var properties = classResource.listProperties(SH.property).mapWith(p -> p.getObject().toString()).toList();
        assertEquals(3, properties.size());
        assertTrue(properties.contains(propertyURI));
    }

    @Test
    void testAppendExistingNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");

        var propertyURI = Constants.DATA_MODEL_NAMESPACE + "test/TestPropertyShape";
        assertThrowsExactly(MappingError.class,
                () -> ClassMapper.mapAppendNodeShapeProperty(classResource, propertyURI, Set.of(propertyURI)));
    }

    @Test
    void testRemoveNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");
        var propertyURI = Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction";

        assertEquals(2, classResource.listProperties(SH.property).toList().size());

        ClassMapper.mapRemoveNodeShapeProperty(model, classResource, propertyURI, Set.of());

        var properties = classResource.listProperties(SH.property).mapWith(p -> p.getObject().toString()).toList();
        assertEquals(1, properties.size());
        assertFalse(properties.contains(propertyURI));
    }

    @Test
    void testMapReferenceResourceAndAddNamespace() {
        var model = ModelFactory.createDefaultModel();
        var uri = DataModelURI.createModelURI("test");

        model.createResource(uri.getModelURI())
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name());

        var dto = new NodeShapeDTO();
        dto.setIdentifier("node-shape-1");
        dto.setTargetNode(Constants.DATA_MODEL_NAMESPACE + "ns-int-1/sub");
        dto.setTargetClass(Constants.DATA_MODEL_NAMESPACE + "ns-int-2/eq");

        ClassMapper.createNodeShapeAndMapToModel(uri, model, dto, EndpointUtils.mockUser);

        // should have namespaces ns-int-1 (application profile) ans ns-int-2 (library) added
        var modelResource = model.getResource(uri.getModelURI());
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "ns-int-1/", modelResource.getProperty(OWL.imports).getObject().toString());
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "ns-int-2/", modelResource.getProperty(DCTerms.requires).getObject().toString());
    }

}
