package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.AssociationRestriction;
import fi.vm.yti.datamodel.api.v2.dto.AttributeRestriction;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.UriDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertyShapeMapperTest {

    @Test
    void testMapAttributeRestrictionToModel() {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");
        var mockUser = EndpointUtils.mockUser;
        var dto = new AttributeRestriction();

        dto.setLabel(Map.of("fi", "PropertyShape label"));
        dto.setIdentifier("ps-1");
        dto.setPath(Constants.DATA_MODEL_NAMESPACE + "test_lib/1.0.0/test_attribute");
        dto.setAllowedValues(List.of("Value 1", "Value 2"));
        dto.setDataType("xsd:integer");
        dto.setDefaultValue("default");
        dto.setHasValue("required value");
        dto.setMaxCount(10);
        dto.setMinCount(1);
        dto.setMaxLength(100);
        dto.setMinLength(3);
        dto.setMinInclusive(5);
        dto.setMaxInclusive(7);
        dto.setMinExclusive(6);
        dto.setMaxExclusive(8);
        dto.setCodeLists(List.of("http://uri.suomi.fi/codelist/test/testcodelist"));
        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToPropertyShapeResource(uri, model, dto, ResourceType.ATTRIBUTE, mockUser);

        var resource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/ps-1");
        var allowedValues = resource.listProperties(SH.in).mapWith((var s) -> s.getObject().toString()).toList();

        assertEquals("PropertyShape label", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test_lib/1.0.0/test_attribute", resource.getProperty(SH.path).getObject().toString());
        assertTrue(MapperUtils.hasType(resource, OWL.DatatypeProperty, SH.PropertyShape));
        assertTrue(allowedValues.containsAll(List.of("Value 1", "Value 2")));
        assertEquals("xsd:integer", MapperUtils.propertyToString(resource, SH.datatype));
        assertEquals("default", MapperUtils.propertyToString(resource, SH.defaultValue));
        assertEquals("required value", MapperUtils.propertyToString(resource, SH.hasValue));
        assertEquals(10, MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        assertEquals(1, MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        assertEquals(100, MapperUtils.getLiteral(resource, SH.maxLength, Integer.class));
        assertEquals(3, MapperUtils.getLiteral(resource, SH.minLength, Integer.class));
        assertEquals(5, MapperUtils.getLiteral(resource, SH.minInclusive, Integer.class));
        assertEquals(7, MapperUtils.getLiteral(resource, SH.maxInclusive, Integer.class));
        assertEquals(6, MapperUtils.getLiteral(resource, SH.minExclusive, Integer.class));
        assertEquals(8, MapperUtils.getLiteral(resource, SH.maxExclusive, Integer.class));
        assertTrue(MapperUtils.arrayPropertyToList(resource, SuomiMeta.codeList).contains("http://uri.suomi.fi/codelist/test/testcodelist"));
    }

    @Test
    void testMapAssociationRestrictionToModel() {
        var model = MapperTestUtils.getModelFromFile("/test_datamodel_profile.ttl");
        var mockUser = EndpointUtils.mockUser;
        var dto = new AssociationRestriction();

        dto.setLabel(Map.of("fi", "PropertyShape label"));
        dto.setIdentifier("ps-1");
        dto.setPath(Constants.DATA_MODEL_NAMESPACE + "test_lib/1.0.0/test_attribute");
        dto.setMaxCount(10);
        dto.setMinCount(1);
        dto.setClassType(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToPropertyShapeResource(uri, model, dto, ResourceType.ASSOCIATION, mockUser);

        var resource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/ps-1");

        assertEquals("PropertyShape label", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test_lib/1.0.0/test_attribute", resource.getProperty(SH.path).getObject().toString());
        assertTrue(MapperUtils.hasType(resource, OWL.ObjectProperty, SH.PropertyShape));
        assertEquals(10, MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        assertEquals(1, MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/TestClass", MapperUtils.propertyToString(resource, SH.class_));

        // should add default sh:path
        model.removeAll(ResourceFactory.createResource(Constants.DATA_MODEL_NAMESPACE + "test/ps-1"), null, null);
        dto.setPath(null);

        ResourceMapper.mapToPropertyShapeResource(uri, model, dto, ResourceType.ASSOCIATION, mockUser);
        resource = model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/ps-1");

        assertEquals(OWL2.topObjectProperty.getURI(), MapperUtils.propertyToString(resource, SH.path));
    }


    @Test
    void testMapToAttributeRestrictionDTO() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAttributeRestriction");
        var dto = ResourceMapper.mapToPropertyShapeInfoDTO(model, uri, MapperTestUtils.getMockOrganizations(),
                false, null);

        assertEquals("test property shape", dto.getLabel().get("fi"));
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("TestAttributeRestriction", dto.getIdentifier());
        assertEquals(new UriDTO(Constants.DATA_MODEL_NAMESPACE + "ytm/some-attribute"), dto.getPath());
        assertNull(dto.getClassType());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals("foo", dto.getDefaultValue());
        assertTrue(dto.getAllowedValues().containsAll(List.of("foo", "bar")));
        assertEquals(1, dto.getMinCount());
        assertEquals(10, dto.getMaxCount());
        assertEquals(2, dto.getMinLength());
        assertEquals(100, dto.getMaxLength());
        assertEquals(5, dto.getMinInclusive());
        assertEquals(7, dto.getMaxInclusive());
        assertEquals(6, dto.getMinExclusive());
        assertEquals(8, dto.getMaxExclusive());
        assertTrue(dto.getCodeLists().contains("http://uri.suomi.fi/codelist/Test"));
        assertEquals("hasValue", dto.getHasValue());
        assertEquals("foo", dto.getDefaultValue());
        assertEquals("xsd:integer", dto.getDataType().getCurie());
        assertEquals(ResourceType.ATTRIBUTE, dto.getType());
    }

    @Test
    void testMapToAssociationRestrictionDTO() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAssociationRestriction");
        var dto = ResourceMapper.mapToPropertyShapeInfoDTO(model, uri, MapperTestUtils.getMockOrganizations(),
                false, null);

        assertEquals("test property shape", dto.getLabel().get("fi"));
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("TestAssociationRestriction", dto.getIdentifier());
        assertEquals(new UriDTO(Constants.DATA_MODEL_NAMESPACE + "ytm/some-attribute"), dto.getPath());
        assertEquals(new UriDTO(Constants.DATA_MODEL_NAMESPACE + "ytm/some-class"), dto.getClassType());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertNull(dto.getDefaultValue());
        assertTrue(dto.getAllowedValues().isEmpty());
        assertNull(dto.getMinLength());
        assertNull(dto.getMaxLength());
        assertNull(dto.getMinExclusive());
        assertNull(dto.getMaxExclusive());
        assertNull(dto.getMinInclusive());
        assertNull(dto.getMaxInclusive());
        assertNull(dto.getHasValue());
        assertNull(dto.getDefaultValue());
        assertNull(dto.getDataType());
        assertEquals(1, dto.getMinCount());
        assertEquals(10, dto.getMaxCount());
        assertEquals(ResourceType.ASSOCIATION, dto.getType());
    }

    @Test
    void testMapToUpdatedAttributeRestrictionResource() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var dto = new AttributeRestriction();

        dto.setLabel(Map.of("fi", "Updated PropertyShape label"));
        dto.setPath(Constants.DATA_MODEL_NAMESPACE + "test/updated_test_attribute");
        dto.setAllowedValues(List.of("Updated value 1"));
        dto.setDataType("xsd:short");
        dto.setDefaultValue("Updated default");
        dto.setHasValue("Updated required value");
        dto.setMaxCount(20);
        dto.setMinCount(2);
        dto.setMaxLength(200);
        dto.setMinLength(5);

        var uri = DataModelURI.createResourceURI("test", "TestAttributeRestriction");
        ResourceMapper.mapToUpdatePropertyShape(uri, model, dto, EndpointUtils.mockUser);

        var resource = model.getResource(uri.getResourceURI());
        var allowedValues = resource.listProperties(SH.in).mapWith((var s) -> s.getObject().toString()).toList();

        assertEquals("Updated PropertyShape label", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/updated_test_attribute", resource.getProperty(SH.path).getObject().toString());
        assertTrue(allowedValues.contains("Updated value 1"));
        assertEquals(1, allowedValues.size());
        assertEquals("xsd:short", resource.getProperty(SH.datatype).getObject().toString());
        assertEquals("Updated default", resource.getProperty(SH.defaultValue).getObject().toString());
        assertEquals("Updated required value", resource.getProperty(SH.hasValue).getObject().toString());
        assertEquals(20, MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        assertEquals(2, MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        assertEquals(200, MapperUtils.getLiteral(resource, SH.maxLength, Integer.class));
        assertEquals(5, MapperUtils.getLiteral(resource, SH.minLength, Integer.class));
    }

    @Test
    void testMapToUpdatedAssociationRestrictionResource() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var dto = new AssociationRestriction();
        dto.setLabel(Map.of("fi", "Updated PropertyShape label"));
        dto.setPath(Constants.DATA_MODEL_NAMESPACE + "test/updated_test_attribute");
        dto.setMaxCount(20);
        dto.setMinCount(2);
        dto.setClassType(Constants.DATA_MODEL_NAMESPACE + "test/TestClass");

        var uri = DataModelURI.createResourceURI("test", "TestAssociationRestriction");
        ResourceMapper.mapToUpdatePropertyShape(uri, model, dto, EndpointUtils.mockUser);

        var resource = model.getResource(uri.getResourceURI());
        assertEquals("Updated PropertyShape label", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/updated_test_attribute", resource.getProperty(SH.path).getObject().toString());
        assertEquals(20, MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        assertEquals(2, MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/TestClass", MapperUtils.propertyToString(resource, SH.class_));
    }

    @Test
    void testMapToCopyToLocalPropertyShape(){
        var sourceModel = MapperTestUtils.getModelFromFile("/models/test_datamodel_profile_with_resources.ttl");
        var targetModel = ModelFactory.createDefaultModel();

        var source = DataModelURI.createResourceURI("test", "DeactivatedPropertyShape");
        var target = DataModelURI.createResourceURI("new", "NewShape");
        ResourceMapper.mapToCopyToLocalPropertyShape(sourceModel, source, targetModel, target, EndpointUtils.mockUser);

        var resource = targetModel.getResource(Constants.DATA_MODEL_NAMESPACE + "new/NewShape");
        assertEquals("NewShape", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals(target.getModelURI(), MapperUtils.propertyToString(resource, RDFS.isDefinedBy));
        assertTrue(MapperUtils.hasType(resource, OWL.DatatypeProperty, SH.PropertyShape));
        assertEquals("deactivated property shape", MapperUtils.localizedPropertyToMap(resource, RDFS.label).get("fi"));
    }
}
