package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class ResourceMapperTest {

    @Test
    void mapToResourceAssociation() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");
        var mockUser = EndpointUtils.mockUser;

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));
        dto.setDomain("http://www.w3.org/2002/07/owl#Class");
        dto.setRange(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass");
        dto.setFunctionalProperty(true);
        dto.setReflexiveProperty(true);
        dto.setTransitiveProperty(true);

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToResource(uri, m, dto, ResourceType.ASSOCIATION, mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource resourceResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertTrue(MapperUtils.hasType(resourceResource, OWL.ObjectProperty));

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resourceResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext/SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(resourceResource, SuomiMeta.creator));
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(resourceResource, SuomiMeta.modifier));

        assertEquals("http://www.w3.org/2002/07/owl#Class", MapperUtils.propertyToString(resourceResource, RDFS.domain));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass", MapperUtils.propertyToString(resourceResource, RDFS.range));

        assertTrue(MapperUtils.hasType(resourceResource, OWL.FunctionalProperty));
        assertTrue(MapperUtils.hasType(resourceResource, OWL2.ReflexiveProperty));
        assertTrue(MapperUtils.hasType(resourceResource, OWL.TransitiveProperty));
    }

    @Test
    void mapToResourceAssociationEmptySubResourceOf() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToResource(uri, m, dto, ResourceType.ASSOCIATION, EndpointUtils.mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource resourceResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.ObjectProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resourceResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topObjectProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttributeEmptySubResourceOf() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToResource(uri, m, dto, ResourceType.ATTRIBUTE, EndpointUtils.mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource resourceResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals(uri.getResourceURI(), modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.DatatypeProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resourceResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topDataProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttribute() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setNote(Map.of("fi", "test note"));
        dto.setDomain("http://www.w3.org/2002/07/owl#Class");
        dto.setRange(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass");
        dto.setFunctionalProperty(true);

        var uri = DataModelURI.createResourceURI("test", dto.getIdentifier());
        ResourceMapper.mapToResource(uri, m, dto, ResourceType.ATTRIBUTE, EndpointUtils.mockUser);

        Resource modelResource = m.getResource(uri.getModelURI());
        Resource resourceResource = m.getResource(uri.getResourceURI());

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals(uri.getResourceURI(), modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertTrue(MapperUtils.hasType(resourceResource, OWL.DatatypeProperty));

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resourceResource, SuomiMeta.publicationStatus)));
        assertEquals(uri.getModelURI(), resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext/SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());

        assertEquals("http://www.w3.org/2002/07/owl#Class", MapperUtils.propertyToString(resourceResource, RDFS.domain));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass", MapperUtils.propertyToString(resourceResource, RDFS.range));

        assertTrue(MapperUtils.hasType(resourceResource, OWL.FunctionalProperty));
    }

    @Test
    void testMapToIndexResourceClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var indexClass = ResourceMapper.mapToIndexResource(m, ModelConstants.SUOMI_FI_NAMESPACE + "test/TestClass");

        assertEquals(ResourceType.CLASS, indexClass.getResourceType());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestClass", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestClass", indexClass.getIdentifier());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getIsDefinedBy());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
        assertEquals("1.0.1", indexClass.getFromVersion());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", indexClass.getVersionIri());
    }

    @Test
    void testMapToIndexResourceAttribute(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");


        var indexClass = ResourceMapper.mapToIndexResource(m, ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttribute");

        assertEquals(ResourceType.ATTRIBUTE, indexClass.getResourceType());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestAttribute", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestAttribute", indexClass.getIdentifier());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getIsDefinedBy());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test attribute", indexClass.getLabel().get("fi"));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/DomainClass", indexClass.getDomain());
        assertEquals("http://www.w3.org/2000/01/rdf-schema#Literal", indexClass.getRange());
        assertEquals("1.0.1", indexClass.getFromVersion());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", indexClass.getVersionIri());
    }

    @Test
    void testMapToIndexResourceAssociation(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");


        var indexClass = ResourceMapper.mapToIndexResource(m, ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAssociation");

        assertEquals(ResourceType.ASSOCIATION, indexClass.getResourceType());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestAssociation", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestAssociation", indexClass.getIdentifier());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getIsDefinedBy());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test association", indexClass.getLabel().get("fi"));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/DomainClass", indexClass.getDomain());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass", indexClass.getRange());
        assertEquals("1.0.1", indexClass.getFromVersion());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/", indexClass.getVersionIri());
    }

    @Test
    void testMapToIndexResourceMinimalClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var indexClass = ResourceMapper.mapToIndexResource(m, uri.getResourceURI());

        assertEquals(ResourceType.CLASS, indexClass.getResourceType());
        assertEquals(uri.getResourceURI(), indexClass.getId());
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals(uri.getResourceId(), indexClass.getIdentifier());
        assertEquals(uri.getModelURI(), indexClass.getIsDefinedBy());
        assertEquals(uri.getModelURI(), indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
        assertNull(indexClass.getNote());
    }

    @Test
    void testMapToIndexResourceMinimalAttribute(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestAttribute");
        var indexResource = ResourceMapper.mapToIndexResource(m, uri.getResourceURI());

        assertEquals(ResourceType.ATTRIBUTE, indexResource.getResourceType());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttribute", indexResource.getId());
        assertEquals(Status.VALID, indexResource.getStatus());
        assertEquals(uri.getResourceId(), indexResource.getIdentifier());
        assertEquals(uri.getModelURI(), indexResource.getIsDefinedBy());
        assertEquals(uri.getModelURI(), indexResource.getNamespace());
        assertEquals(1, indexResource.getLabel().size());
        assertEquals("test attribute", indexResource.getLabel().get("fi"));
        assertNull(indexResource.getNote());
    }

    @Test
    void testMapToIndexResourceMinimalAssociation(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        var indexResource = ResourceMapper.mapToIndexResource(m, uri.getResourceURI());

        assertEquals(ResourceType.ASSOCIATION, indexResource.getResourceType());
        assertEquals(uri.getResourceURI(), indexResource.getId());
        assertEquals(Status.VALID, indexResource.getStatus());
        assertEquals(uri.getResourceId(), indexResource.getIdentifier());
        assertEquals(uri.getModelURI(), indexResource.getIsDefinedBy());
        assertEquals(uri.getModelURI(), indexResource.getNamespace());
        assertEquals(1, indexResource.getLabel().size());
        assertEquals("test association", indexResource.getLabel().get("fi"));
        assertNull(indexResource.getNote());
    }

    @Test
    void mapToResourceInfoDTOAttribute() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAttribute");
        var dto = ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null);

        assertEquals(ResourceType.ATTRIBUTE, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test attribute", dto.getLabel().get("fi"));
        assertEquals("comment visible for admin", dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(1, dto.getSubResourceOf().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubResource"), dto.getSubResourceOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getEquivalentResource().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqResource"), dto.getEquivalentResource().stream().findFirst().orElse(null));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals("TestAttribute", dto.getIdentifier());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestAttribute", dto.getUri());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/DomainClass", dto.getDomain().getUri());
        assertEquals("test:DomainClass", dto.getDomain().getCurie());
        assertEquals("http://www.w3.org/2000/01/rdf-schema#Literal", dto.getRange().getUri());
        assertEquals("rdfs:Literal", dto.getRange().getCurie());
        assertFalse(dto.getFunctionalProperty());
        assertNull(dto.getReflexiveProperty());
        assertNull(dto.getTransitiveProperty());
    }

    @Test
    void mapToResourceInfoDTOAssociation() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        var dto = ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null);

        assertEquals(ResourceType.ASSOCIATION, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test association", dto.getLabel().get("fi"));
        assertEquals("comment visible for admin", dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(1, dto.getSubResourceOf().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubResource"), dto.getSubResourceOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getEquivalentResource().size());
        assertEquals(new UriDTO(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqResource"), dto.getEquivalentResource().stream().findFirst().orElse(null));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals("TestAssociation", dto.getIdentifier());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/TestAssociation", dto.getUri());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/DomainClass", dto.getDomain().getUri());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/1.0.1/RangeClass", dto.getRange().getUri());
        assertTrue(dto.getFunctionalProperty());
        assertFalse(dto.getTransitiveProperty());
        assertFalse(dto.getReflexiveProperty());
    }

    @Test
    void mapToResourceInfoDTOAttributeMinimal() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAttribute");
        var dto = ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null);

        assertEquals(ResourceType.ATTRIBUTE, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test attribute", dto.getLabel().get("fi"));
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(0, dto.getSubResourceOf().size());
        assertEquals(0, dto.getEquivalentResource().size());
        assertNull(dto.getSubject());
        assertEquals("TestAttribute", dto.getIdentifier());
        assertEquals(0, dto.getNote().size());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttribute", dto.getUri());
    }

    @Test
    void mapToResourceInfoDTOAssociationMinimal() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        var dto = ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null);

        assertEquals(ResourceType.ASSOCIATION, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test association", dto.getLabel().get("fi"));
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(0, dto.getSubResourceOf().size());
        assertEquals(0, dto.getEquivalentResource().size());
        assertNull(dto.getSubject());
        assertEquals("TestAssociation", dto.getIdentifier());
        assertEquals(0, dto.getNote().size());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("Yhteentoimivuusalustan yllapito", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals(MapperTestUtils.TEST_ORG_ID.toString(), dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAssociation", dto.getUri());
    }



    @Test
    void failMapToResourceInfoDTOClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        assertThrowsExactly(MappingError.class, () -> ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null));
    }

    @Test
    void failMapToResourceInfoDTOClassMinimal(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestClass");
        assertThrowsExactly(MappingError.class, () -> ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, null));
    }

    @Test
    void mapToResourceInfoAuthenticatedUser() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        Consumer<ResourceCommonDTO> userMapper = (var dto) -> {
            var creator = new UserDTO("123");
            var modifier = new UserDTO("123");
            creator.setName("creator fake-user");
            modifier.setName("modifier fake-user");
            dto.setCreator(creator);
            dto.setModifier(modifier);
        };
        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        var dto = ResourceMapper.mapToResourceInfoDTO(m, uri, MapperTestUtils.getMockOrganizations(), true, userMapper);

        assertEquals("creator fake-user", dto.getCreator().getName());
        assertEquals("modifier fake-user", dto.getModifier().getName());
    }

    @Test
    void mapToUpdateModelAttribute() {
        var uri = DataModelURI.createResourceURI("test", "TestAttribute");
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var resource = m.getResource(uri.getResourceURI());
        var mockUser = EndpointUtils.mockUser;

        var dto = new ResourceDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setDomain(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewDomainClass");
        dto.setRange("xsd:integer");
        dto.setFunctionalProperty(true);

        assertTrue(MapperUtils.hasType(resource, OWL.DatatypeProperty));
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test attribute", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals(uri.getResourceId(), resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqResource", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/DomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("http://www.w3.org/2000/01/rdf-schema#Literal", MapperUtils.propertyToString(resource, RDFS.range));
        assertFalse(MapperUtils.hasType(resource, OWL.FunctionalProperty));

        ResourceMapper.mapToUpdateResource(uri, m, dto, mockUser);

        assertTrue(MapperUtils.hasType(resource, OWL.DatatypeProperty));
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAttribute", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("https://www.example.com/ns/ext/NewSub", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals(mockUser.getId().toString(), MapperUtils.propertyToString(resource, SuomiMeta.modifier));
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", MapperUtils.propertyToString(resource, SuomiMeta.creator));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewDomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("xsd:integer", MapperUtils.propertyToString(resource, RDFS.range));
        assertTrue(MapperUtils.hasType(resource, OWL.FunctionalProperty));
    }

    @Test
    void mapToUpdateModelAssociation() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        var resource = m.getResource(uri.getResourceURI());

        var dto = new ResourceDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setDomain(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewDomainClass");
        dto.setRange(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewRangeClass");
        dto.setFunctionalProperty(true);
        dto.setTransitiveProperty(true);
        dto.setReflexiveProperty(true);

        assertTrue(MapperUtils.hasType(resource, OWL.ObjectProperty));
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test association", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAssociation", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/EqResource", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.VALID, MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/DomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/RangeClass", MapperUtils.propertyToString(resource, RDFS.range));
        assertTrue(MapperUtils.hasType(resource, OWL.FunctionalProperty));
        assertFalse(MapperUtils.hasType(resource, OWL2.ReflexiveProperty));
        assertFalse(MapperUtils.hasType(resource, OWL2.TransitiveProperty));

        ResourceMapper.mapToUpdateResource(uri, m, dto, EndpointUtils.mockUser);

        assertTrue(MapperUtils.hasType(resource, OWL.ObjectProperty));
        assertEquals(uri.getModelURI(), resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAssociation", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "int/NewEq", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("https://www.example.com/ns/ext/NewSub", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewDomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewRangeClass", MapperUtils.propertyToString(resource, RDFS.range));
        assertTrue(MapperUtils.hasType(resource, OWL.FunctionalProperty));
        assertTrue(MapperUtils.hasType(resource, OWL2.ReflexiveProperty));
        assertTrue(MapperUtils.hasType(resource, OWL.TransitiveProperty));
    }

    @Test
    void mapToUpdateAttributeEmptySubResource(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttribute");

        var uri = DataModelURI.createResourceURI("test", "TestAttribute");
        //null values get removed, so it will change to topObjectProperty
        ResourceMapper.mapToUpdateResource(uri, m, new ResourceDTO(), EndpointUtils.mockUser);
        assertEquals(OWL2.topDataProperty, resource.getProperty(RDFS.subPropertyOf).getResource());

        var dto = new ResourceDTO();
        dto.setSubResourceOf(Set.of());

        m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        resource = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAttribute");

        //should change if empty
        ResourceMapper.mapToUpdateResource(uri, m, dto, EndpointUtils.mockUser);
        assertEquals(OWL2.topDataProperty, resource.getProperty(RDFS.subPropertyOf).getResource());
    }

    @Test
    void mapToUpdateAssociationEmptySubResource(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAssociation");

        var uri = DataModelURI.createResourceURI("test", "TestAssociation");
        //null values get removed, so it will change to topObjectProperty
        ResourceMapper.mapToUpdateResource(uri, m, new ResourceDTO(), EndpointUtils.mockUser);
        assertEquals(OWL2.topObjectProperty, resource.getProperty(RDFS.subPropertyOf).getResource());

        var dto = new ResourceDTO();
        dto.setSubResourceOf(Set.of());

        m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        resource = m.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "test/TestAssociation");

        //should change if empty
        ResourceMapper.mapToUpdateResource(uri, m, dto, EndpointUtils.mockUser);
        assertEquals(OWL2.topObjectProperty, resource.getProperty(RDFS.subPropertyOf).getResource());
    }

    @Test
    void mapExternalResource() {
        var m = MapperTestUtils.getModelFromFile("/external_class.ttl");
        var dto = ResourceMapper.mapToExternalResource(m.getResource("http://purl.org/ontology/mo/encoding"));

        assertEquals("encoding", dto.getLabel().get("en"));
        assertEquals("http://purl.org/ontology/mo/encoding", dto.getUri());
    }

    @Test
    void mapIndexResourceInfo() {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + "test";
        var indexResource = new IndexResource();
        indexResource.setId(modelURI + "/class-1");
        indexResource.setNamespace("test");
        indexResource.setLabel(Map.of("fi", "Test resource"));
        indexResource.setResourceType(ResourceType.CLASS);
        indexResource.setIsDefinedBy(modelURI);
        indexResource.setNote(Map.of("fi", "Test description"));
        indexResource.setStatus(Status.VALID);
        indexResource.setSubject("http://uri.suomi.fi/terminology/dd0e10ed/concept-1");

        var indexModel = new IndexModel();
        indexModel.setLabel(Map.of("fi", "Data model"));
        indexModel.setStatus(Status.VALID);
        indexModel.setIsPartOf(List.of("P1", "P2"));
        indexModel.setType(ModelType.LIBRARY);
        indexModel.setVersion("1.0.1");

        var dataModels = Map.of(modelURI, indexModel);

        var conceptsModel = MapperTestUtils.getModelFromFile("/all_concepts.ttl");
        var result = ResourceMapper.mapIndexResourceInfo(indexResource, dataModels, conceptsModel);

        assertEquals("Test resource", result.getLabel().get("fi"));
        assertEquals("Test description", result.getNote().get("fi"));
        assertEquals(Status.VALID, result.getStatus());
        assertEquals(modelURI + "/class-1", result.getId());
        assertEquals(ResourceType.CLASS, result.getResourceType());
        assertEquals("test", result.getNamespace());

        var dataModel = result.getDataModelInfo();
        assertEquals("Data model", dataModel.getLabel().get("fi"));
        assertEquals(ModelType.LIBRARY, dataModel.getModelType());
        assertEquals(Status.VALID, dataModel.getStatus());
        assertTrue(dataModel.getGroups().containsAll(List.of("P1", "P2")));
        assertEquals("1.0.1", dataModel.getVersion());

        var concept = result.getConceptInfo();
        assertEquals("käsite", concept.getConceptLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/dd0e10ed/concept-1", concept.getConceptURI());
        assertEquals("Testisanasto", concept.getTerminologyLabel().get("fi"));
    }

    @Test
    void testMapExternalIndexClasses() {
        var model = MapperTestUtils.getModelFromFile("/external_resources.ttl");

        var resource1 = model.getResource("http://www.w3.org/ns/oa#describing");
        var resource2 = model.getResource("http://www.w3.org/ns/oa#Motivation");

        var indexResource1 = ResourceMapper.mapExternalToIndexResource(resource1);
        var indexResource2 = ResourceMapper.mapExternalToIndexResource(resource2);

        assertNotNull(indexResource1);
        assertNotNull(indexResource2);

        assertEquals(ResourceType.CLASS, indexResource1.getResourceType());
        assertEquals(ResourceType.CLASS, indexResource2.getResourceType());

        assertEquals("http://www.w3.org/ns/oa#describing", indexResource1.getId());
        assertEquals("describing", indexResource1.getIdentifier());
        assertEquals("http://www.w3.org/ns/oa#", indexResource1.getNamespace());
        assertEquals("http://www.w3.org/ns/oa#", indexResource1.getIsDefinedBy());
        assertEquals("Label describing", indexResource1.getLabel().get("en"));
        assertEquals("Test comment describing", indexResource1.getNote().get("en"));
        assertEquals("oa:describing", indexResource1.getCurie());
    }

    @Test
    void testMapExternalIndexResources() {
        var model = MapperTestUtils.getModelFromFile("/external_resources.ttl");
        var resource1 = model.getResource("http://www.w3.org/ns/oa#exact");
        var resource2 = model.getResource("http://www.w3.org/ns/oa#hasEndSelector");

        var indexResource1 = ResourceMapper.mapExternalToIndexResource(resource1);
        var indexResource2 = ResourceMapper.mapExternalToIndexResource(resource2);

        assertNotNull(indexResource1);
        assertNotNull(indexResource2);

        assertEquals(ResourceType.ATTRIBUTE, indexResource1.getResourceType());
        assertEquals(ResourceType.ASSOCIATION, indexResource2.getResourceType());
    }

    @Test
    void testUpdateRestrictionsAfterRangeChange() {
        var model = MapperTestUtils.getModelFromFile("/model_with_owl_restrictions.ttl");

        var uri = DataModelURI.createResourceURI("model", "attribute-1");
        var newRange = XSD.integer.getURI();

        // update range to xsd:integer
        var dto = new ResourceDTO();
        dto.setIdentifier(uri.getResourceId());
        dto.setLabel(Map.of("fi", "test label"));
        dto.setRange(newRange);

        ResourceMapper.mapToUpdateResource(uri, model, dto, EndpointUtils.mockUser);

        var classResource = model.getResource(uri.getModelURI() + "class-1");
        var list = classResource.getProperty(OWL.equivalentClass).getObject()
                .asResource().getProperty(OWL.intersectionOf).getObject().as(RDFList.class);

        var updated = list.asJavaList().stream()
                .filter(r -> r.asResource().getProperty(OWL.onProperty).getObject().toString()
                        .equals(ModelConstants.SUOMI_FI_NAMESPACE + "model/attribute-1"))
                .findFirst();
        assertTrue(updated.isPresent());
        assertEquals(newRange, updated.get().asResource().getProperty(OWL.someValuesFrom).getObject().toString());
    }

    @Test
    void testMapReferenceResourceAndAddNamespace() {
        var model = ModelFactory.createDefaultModel();
        var uri = DataModelURI.createResourceURI("test-library", "attr-1");
        model.createResource(uri.getModelURI())
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name());


        var attrDTO = new ResourceDTO();
        attrDTO.setIdentifier("attr-1");
        attrDTO.setSubResourceOf(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-1/sub"));
        attrDTO.setEquivalentResource(Set.of(ModelConstants.SUOMI_FI_NAMESPACE + "ns-int-2/eq"));

        ResourceMapper.mapToResource(uri, model, attrDTO, ResourceType.ATTRIBUTE, EndpointUtils.mockUser);

        assertEquals(2, model.getResource(uri.getModelURI()).listProperties(OWL.imports).toList().size());
    }

    @Test
    void testMapReferencePropertyShapeAndAddNamespace() {
        var model = ModelFactory.createDefaultModel();

        var uri = DataModelURI.createResourceURI("test-profile", "ps-1");

        model.createResource(uri.getModelURI())
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile)
                .addProperty(SuomiMeta.publicationStatus, Status.DRAFT.name());

        var propertyShape = new AssociationRestriction();
        propertyShape.setIdentifier("ps-1");
        propertyShape.setPath(ModelConstants.SUOMI_FI_NAMESPACE + "/test_lib_2/path");
        propertyShape.setClassType(ModelConstants.SUOMI_FI_NAMESPACE + "test_lib_2/class");

        ResourceMapper.mapToPropertyShapeResource(uri, model, propertyShape, ResourceType.ASSOCIATION, EndpointUtils.mockUser);

        assertEquals(2, model.getResource(uri.getModelURI()).listProperties(DCTerms.requires).toList().size());
    }
}
