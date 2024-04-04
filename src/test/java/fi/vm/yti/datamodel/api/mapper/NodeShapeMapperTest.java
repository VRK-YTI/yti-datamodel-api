package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.datatypes.xsd.XSDDatatype;
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
        var propertiesQueryResult = MapperTestUtils.getModelFromFile("/properties_result.ttl");
        var mockUser = EndpointUtils.mockUser;

        // assume there is a resource with identifier association-1 defined in the model
        // suffix -1 should be added when creating new property shape resource
        Predicate<String> freePrefixCheck = s -> s.equals(ModelConstants.SUOMI_FI_NAMESPACE + "test/association-1");

        var dto = new NodeShapeDTO();
        dto.setIdentifier("TestClass");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));
        dto.setTargetClass(ModelConstants.SUOMI_FI_NAMESPACE + "target/Class");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        ClassMapper.createNodeShapeAndMapToModel(uri, model, dto, mockUser);
        ClassMapper.mapPlaceholderPropertyShapes(model, uri.getResourceURI(),
                propertiesQueryResult, mockUser, freePrefixCheck);

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
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "target/Class", classResource.getProperty(SH.targetClass).getObject().toString());

        assertEquals(2, classResource.listProperties(SH.property).toList().size());
        var propertyShapeAttribute = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/attribute-1");
        var propertyShapeAssociation = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/association-1-1");

        assertNotNull(propertyShapeAttribute);
        assertNotNull(propertyShapeAssociation);

        // PropertyShapes should have two type properties
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, OWL.DatatypeProperty));

        assertTrue(MapperUtils.hasType(propertyShapeAssociation, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAssociation, OWL.ObjectProperty));

        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test_lib/1.0.0/attribute-1",
                propertyShapeAttribute.getProperty(SH.path).getObject().toString());
        assertEquals(Status.DRAFT, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(propertyShapeAttribute, SuomiMeta.publicationStatus)));
        assertEquals("Attribute attribute-1", MapperUtils.localizedPropertyToMap(propertyShapeAttribute, RDFS.label).get("fi"));
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
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass", dto.getUri());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "target/Class"), dto.getTargetClass());
    }

    @Test
    void testMapReferencePropertyShapes() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        ClassMapper.mapNodeShapeProperties(model, ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass",
                Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "test_lib/attribute-1", ModelConstants.SUOMI_FI_NAMESPACE + "test_lib/association-1")
        );

        var classRes = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass");

        var propertyShapes = classRes.listProperties(SH.property).mapWith(p -> p.getObject().toString()).toList();

        // should contain both existing in the model and those found from with query
        assertEquals(4, propertyShapes.size());
        assertTrue(propertyShapes.containsAll(List.of(
                ModelConstants.SUOMI_FI_NAMESPACE + "test_lib/attribute-1",
                ModelConstants.SUOMI_FI_NAMESPACE + "test_lib/association-1",
                ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttributeRestriction",
                ModelConstants.SUOMI_FI_NAMESPACE + "test/DeactivatedPropertyShape"))
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
        dto.setTargetClass(ModelConstants.SUOMI_FI_NAMESPACE + "target/NewClass");

        assertEquals(SH.NodeShape, resource.getProperty(RDF.type).getResource());
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "target/Class", resource.getProperty(SH.targetClass).getObject().toString());

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

        var propertyShape1 = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttributeRestriction");
        var propertyShape2 = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/DeactivatedPropertyShape");

        assertFalse(propertyShape1.hasProperty(SH.deactivated));
        assertTrue(propertyShape2.getProperty(SH.deactivated).getObject().asLiteral().getBoolean());

        ClassMapper.toggleAndMapDeactivatedProperty(m, ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttributeRestriction", false);
        ClassMapper.toggleAndMapDeactivatedProperty(m, ModelConstants.SUOMI_FI_NAMESPACE + "test/DeactivatedPropertyShape", false);

        assertTrue(propertyShape1.getProperty(SH.deactivated).getObject().asLiteral().getBoolean());
        assertFalse(propertyShape2.hasProperty(SH.deactivated));
    }

    @Test
    void testAppendNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass");
        var propertyURI = ModelConstants.SUOMI_FI_NAMESPACE + "foo";

        assertEquals(2, classResource.listProperties(SH.property).toList().size());

        ClassMapper.mapAppendNodeShapeProperty(classResource, propertyURI, Set.of());

        var properties = classResource.listProperties(SH.property).mapWith(p -> p.getObject().toString()).toList();
        assertEquals(3, properties.size());
        assertTrue(properties.contains(propertyURI));
    }

    @Test
    void testAppendExistingNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass");

        var propertyURI = ModelConstants.SUOMI_FI_NAMESPACE + "test/TestPropertyShape";
        assertThrowsExactly(MappingError.class,
                () -> ClassMapper.mapAppendNodeShapeProperty(classResource, propertyURI, Set.of(propertyURI)));
    }

    @Test
    void testRemoveNodeShapeProperty() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var classResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass");
        var propertyURI = ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttributeRestriction";

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
        dto.setTargetNode(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/sub");
        dto.setTargetClass(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-2/eq");

        ClassMapper.createNodeShapeAndMapToModel(uri, model, dto, EndpointUtils.mockUser);

        // should have namespaces ns-int-1 (application profile) ans ns-int-2 (library) added
        var modelResource = model.getResource(uri.getModelURI());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/", modelResource.getProperty(OWL.imports).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-2/", modelResource.getProperty(DCTerms.requires).getObject().toString());
    }

}
