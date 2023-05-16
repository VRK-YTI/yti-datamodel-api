package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class NodeShapeMapperTest {

    @Test
    void testCreateNodeShapeAndMapToProfile() {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");
        var propertiesQueryResult = MapperTestUtils.getModelFromFile("/properties_result.ttl");
        var mockUser = EndpointUtils.mockUser;

        ClassDTO dto = new ClassDTO();
        dto.setIdentifier("TestClass");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));
        dto.setTargetClass("http://uri.suomi.fi/datamodel/ns/target#Class");

        ClassMapper.createClassAndMapToModel("http://uri.suomi.fi/datamodel/ns/test", model, dto, mockUser);
        ClassMapper.mapPlaceholderPropertyShapes(model, "http://uri.suomi.fi/datamodel/ns/test#TestClass",
                propertiesQueryResult, mockUser);

        Resource modelResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource classResource = model.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#TestClass", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(1, classResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", classResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", classResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(SH.NodeShape, classResource.getProperty(RDF.type).getResource());
        assertEquals(Status.DRAFT, Status.valueOf(classResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", classResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, classResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("TestClass", classResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        assertEquals("test note", classResource.getProperty(SH.description).getLiteral().getString());
        assertEquals(mockUser.getId().toString(), classResource.getProperty(Iow.creator).getString());
        assertEquals(mockUser.getId().toString(), classResource.getProperty(Iow.modifier).getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/target#Class", classResource.getProperty(SH.targetClass).getObject().toString());

        assertEquals(2, classResource.listProperties(SH.property).toList().size());
        var propertyShapeAttribute = model.getResource("http://uri.suomi.fi/datamodel/ns/test#attribute-1");
        var propertyShapeAssociation = model.getResource("http://uri.suomi.fi/datamodel/ns/test#association-1");

        assertNotNull(propertyShapeAttribute);
        assertNotNull(propertyShapeAssociation);

        // PropertyShapes should have two type properties
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAttribute, OWL.DatatypeProperty));

        assertTrue(MapperUtils.hasType(propertyShapeAssociation, SH.PropertyShape));
        assertTrue(MapperUtils.hasType(propertyShapeAssociation, OWL.ObjectProperty));

        assertEquals("http://uri.suomi.fi/datamodel/ns/test_lib#attribute-1",
                propertyShapeAttribute.getProperty(SH.path).getObject().toString());
        assertEquals(Status.DRAFT.name(), propertyShapeAttribute.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("Attribute attribute-1", MapperUtils.localizedPropertyToMap(propertyShapeAttribute, RDFS.label).get("fi"));
    }

    @Test
    void testMapNodeShapeToClassDTO(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var dto = ClassMapper.mapToClassDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestClass", MapperTestUtils.getOrgModel(), false, null);

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
        assertEquals("test org", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#TestClass", dto.getUri());
        assertEquals("http://uri.suomi.fi/datamodel/ns/target#Class", dto.getTargetClass());
    }

    @Test
    void testMapToUpdateClassProfile(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");
        var mockUser = EndpointUtils.mockUser;

        var dto = new ClassDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setStatus(Status.INVALID);
        dto.setNote(Map.of("fi", "new note"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setTargetClass("http://uri.suomi.fi/datamodel/ns/target#NewClass");

        assertEquals(SH.NodeShape, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(SH.description).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/target#Class", resource.getProperty(SH.targetClass).getObject().toString());

        ClassMapper.mapToUpdateClass(m, "http://uri.suomi.fi/datamodel/ns/test", resource, dto, mockUser);

        assertEquals(SH.NodeShape, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(Status.INVALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(SH.description).toList().size());
        assertEquals("new note", resource.getProperty(SH.description).getLiteral().getString());
        assertEquals("fi", resource.getProperty(SH.description).getLiteral().getLanguage());
        assertEquals(mockUser.getId().toString(), resource.getProperty(Iow.modifier).getObject().toString());
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", resource.getProperty(Iow.creator).getObject().toString());
    }

}
