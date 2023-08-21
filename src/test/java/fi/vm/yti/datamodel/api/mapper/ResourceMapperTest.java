package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
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
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));
        dto.setDomain("http://www.w3.org/2002/07/owl#Class");
        dto.setRange("http://uri.suomi.fi/datamodel/ns/test/RangeClass");

        ResourceMapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto, ResourceType.ASSOCIATION, mockUser);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.ObjectProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext/SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals(mockUser.getId().toString(), resourceResource.getProperty(Iow.creator).getObject().toString());
        assertEquals(mockUser.getId().toString(), resourceResource.getProperty(Iow.modifier).getObject().toString());

        assertEquals("http://www.w3.org/2002/07/owl#Class", MapperUtils.propertyToString(resourceResource, RDFS.domain));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/RangeClass", MapperUtils.propertyToString(resourceResource, RDFS.range));
    }

    @Test
    void mapToResourceAssociationEmptySubResourceOf() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        ResourceMapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto, ResourceType.ASSOCIATION, EndpointUtils.mockUser);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.ObjectProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topObjectProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttributeEmptySubResourceOf() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        ResourceMapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto, ResourceType.ATTRIBUTE, EndpointUtils.mockUser);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.DatatypeProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topDataProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttribute() {
        var m = MapperTestUtils.getModelFromFile("/test_datamodel_library.ttl");

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));
        dto.setDomain("http://www.w3.org/2002/07/owl#Class");
        dto.setRange("http://uri.suomi.fi/datamodel/ns/test/RangeClass");

        ResourceMapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto, ResourceType.ATTRIBUTE, EndpointUtils.mockUser);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/Resource", modelResource.getProperty(DCTerms.hasPart).getObject().toString());

        assertEquals(OWL.DatatypeProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(RDFS.comment).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext/SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());

        assertEquals("http://www.w3.org/2002/07/owl#Class", MapperUtils.propertyToString(resourceResource, RDFS.domain));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/RangeClass", MapperUtils.propertyToString(resourceResource, RDFS.range));
    }

    @Test
    void testMapToIndexResourceClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var indexClass = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestClass");

        assertEquals(ResourceType.CLASS, indexClass.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestClass", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestClass", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
    }

    @Test
    void testMapToIndexResourceAttribute(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");


        var indexClass = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestAttribute");

        assertEquals(ResourceType.ATTRIBUTE, indexClass.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAttribute", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestAttribute", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test attribute", indexClass.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", indexClass.getDomain());
        assertEquals("rdf:Literal", indexClass.getRange());
    }

    @Test
    void testMapToIndexResourceAssociation(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");


        var indexClass = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestAssociation");

        assertEquals(ResourceType.ASSOCIATION, indexClass.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAssociation", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestAssociation", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test association", indexClass.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", indexClass.getDomain());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/RangeClass", indexClass.getRange());
    }

    @Test
    void testMapToIndexResourceMinimalClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var indexClass = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestClass");

        assertEquals(ResourceType.CLASS, indexClass.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestClass", indexClass.getId());
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestClass", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
        assertNull(indexClass.getNote());
    }

    @Test
    void testMapToIndexResourceMinimalAttribute(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var indexResource = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestAttribute");

        assertEquals(ResourceType.ATTRIBUTE, indexResource.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAttribute", indexResource.getId());
        assertEquals(Status.VALID, indexResource.getStatus());
        assertEquals("TestAttribute", indexResource.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexResource.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexResource.getNamespace());
        assertEquals(1, indexResource.getLabel().size());
        assertEquals("test attribute", indexResource.getLabel().get("fi"));
        assertNull(indexResource.getNote());
    }

    @Test
    void testMapToIndexResourceMinimalAssociation(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var indexResource = ResourceMapper.mapToIndexResource(m, "http://uri.suomi.fi/datamodel/ns/test/TestAssociation");

        assertEquals(ResourceType.ASSOCIATION, indexResource.getResourceType());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAssociation", indexResource.getId());
        assertEquals(Status.VALID, indexResource.getStatus());
        assertEquals("TestAssociation", indexResource.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexResource.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/", indexResource.getNamespace());
        assertEquals(1, indexResource.getLabel().size());
        assertEquals("test association", indexResource.getLabel().get("fi"));
        assertNull(indexResource.getNote());
    }

    @Test
    void mapToResourceInfoDTOAttribute() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var dto = ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestAttribute", getOrgModel(), true, null);

        assertEquals(ResourceType.ATTRIBUTE, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test attribute", dto.getLabel().get("fi"));
        assertEquals("comment visible for admin", dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(1, dto.getSubResourceOf().size());
        assertEquals(new UriDTO("http://uri.suomi.fi/datamodel/ns/test/SubResource"), dto.getSubResourceOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getEquivalentResource().size());
        assertEquals(new UriDTO("http://uri.suomi.fi/datamodel/ns/test/EqResource"), dto.getEquivalentResource().stream().findFirst().orElse(null));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals("TestAttribute", dto.getIdentifier());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("test org", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAttribute", dto.getUri());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", dto.getDomain().getUri());
        assertEquals("test:DomainClass", dto.getDomain().getCurie());
        assertEquals("rdf:Literal", dto.getRange().getUri());
        assertEquals("rdf:Literal", dto.getRange().getCurie());
    }

    @Test
    void mapToResourceInfoDTOAssociation() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var dto = ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestAssociation", getOrgModel(), true, null);

        assertEquals(ResourceType.ASSOCIATION, dto.getType());
        assertEquals(1, dto.getLabel().size());
        assertEquals("test association", dto.getLabel().get("fi"));
        assertEquals("comment visible for admin", dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(1, dto.getSubResourceOf().size());
        assertEquals(new UriDTO("http://uri.suomi.fi/datamodel/ns/test/SubResource"), dto.getSubResourceOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getEquivalentResource().size());
        assertEquals(new UriDTO("http://uri.suomi.fi/datamodel/ns/test/EqResource"), dto.getEquivalentResource().stream().findFirst().orElse(null));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject().getConceptURI());
        assertEquals("TestAssociation", dto.getIdentifier());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
        assertEquals("2023-02-03T11:46:36.404Z", dto.getModified());
        assertEquals("2023-02-03T11:46:36.404Z", dto.getCreated());
        assertEquals("test org", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAssociation", dto.getUri());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", dto.getDomain().getUri());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/RangeClass", dto.getRange().getUri());
    }

    @Test
    void mapToResourceInfoDTOAttributeMinimal() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");

        var dto = ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestAttribute", getOrgModel(), true, null);

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
        assertEquals("test org", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAttribute", dto.getUri());
    }

    @Test
    void mapToResourceInfoDTOAssociationMinimal() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");


        var dto = ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestAssociation", getOrgModel(), true, null);

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
        assertEquals("test org", dto.getContributor().stream().findFirst().orElseThrow().getLabel().get("fi"));
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getContributor().stream().findFirst().orElseThrow().getId());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestAssociation", dto.getUri());
    }



    @Test
    void failMapToResourceInfoDTOClass(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        assertThrowsExactly(MappingError.class, () -> ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestClass", getOrgModel(), true, null));
    }

    @Test
    void failMapToResourceInfoDTOClassMinimal(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_with_minimal_resources.ttl");
        assertThrowsExactly(MappingError.class, () -> ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestClass", getOrgModel(), true, null));
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
        var dto = ResourceMapper.mapToResourceInfoDTO(m, "http://uri.suomi.fi/datamodel/ns/test", "TestAssociation", getOrgModel(), true, userMapper);

        assertEquals("creator fake-user", dto.getCreator().getName());
        assertEquals("modifier fake-user", dto.getModifier().getName());
    }

    @Test
    void mapToUpdateModelAttribute() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestAttribute");
        var mockUser = EndpointUtils.mockUser;

        var dto = new ResourceDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setStatus(Status.INVALID);
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/NewEq"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setDomain("http://uri.suomi.fi/datamodel/ns/test/NewDomainClass");
        dto.setRange("xsd:integer");

        assertEquals(OWL.DatatypeProperty, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test attribute", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAttribute", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/EqResource", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("rdf:Literal", MapperUtils.propertyToString(resource, RDFS.range));


        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAttribute", dto, mockUser);

        assertEquals(OWL.DatatypeProperty, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAttribute", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/NewEq", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("https://www.example.com/ns/ext/NewSub", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.INVALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals(mockUser.getId().toString(), resource.getProperty(Iow.modifier).getObject().toString());
        assertEquals("2a5c075f-0d0e-4688-90e0-29af1eebbf6d", resource.getProperty(Iow.creator).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/NewDomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("xsd:integer", MapperUtils.propertyToString(resource, RDFS.range));
    }

    @Test
    void mapToUpdateModelAssociation() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestAssociation");

        var dto = new ResourceDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setStatus(Status.INVALID);
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int/NewEq"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext/NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");
        dto.setDomain("http://uri.suomi.fi/datamodel/ns/test/NewDomainClass");
        dto.setRange("http://uri.suomi.fi/datamodel/ns/test/NewRangeClass");

        assertEquals(OWL.ObjectProperty, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test association", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAssociation", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/EqResource", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/DomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/RangeClass", MapperUtils.propertyToString(resource, RDFS.range));

        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAssociation", dto, EndpointUtils.mockUser);

        assertEquals(OWL.ObjectProperty, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestAssociation", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int/NewEq", resource.getProperty(OWL.equivalentProperty).getObject().toString());
        assertEquals("https://www.example.com/ns/ext/NewSub", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(Status.INVALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(RDFS.comment).toList().size());
        assertEquals("new note", resource.getProperty(RDFS.comment).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.comment).getLiteral().getLanguage());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/NewDomainClass", MapperUtils.propertyToString(resource, RDFS.domain));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/NewRangeClass", MapperUtils.propertyToString(resource, RDFS.range));
    }

    @Test
    void mapToUpdateAttributeEmptySubResource(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestAttribute");

        //Shouldn't change if null
        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAttribute", new ResourceDTO(), EndpointUtils.mockUser);
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());

        var dto = new ResourceDTO();
        dto.setSubResourceOf(Set.of());

        //should change if empty
        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAttribute", dto, EndpointUtils.mockUser);
        assertEquals("http://www.w3.org/2002/07/owl#topDataProperty", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
    }

    @Test
    void mapToUpdateAssociationEmptySubResource(){
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestAssociation");

        //Shouldn't change if null
        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAssociation", new ResourceDTO(), EndpointUtils.mockUser);
        assertEquals("http://uri.suomi.fi/datamodel/ns/test/SubResource", resource.getProperty(RDFS.subPropertyOf).getObject().toString());

        var dto = new ResourceDTO();
        dto.setSubResourceOf(Set.of());

        //should change if empty
        ResourceMapper.mapToUpdateResource("http://uri.suomi.fi/datamodel/ns/test", m, "TestAssociation", dto, EndpointUtils.mockUser);
        assertEquals("http://www.w3.org/2002/07/owl#topObjectProperty", resource.getProperty(RDFS.subPropertyOf).getObject().toString());
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
        var modelURI = "http://uri.suomi.fi/datamodel/ns/test";
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

        var indexResource1 = ResourceMapper.mapExternalToIndexResource(model, resource1);
        var indexResource2 = ResourceMapper.mapExternalToIndexResource(model, resource2);

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
    }

    @Test
    void testMapExternalIndexResources() {
        var model = MapperTestUtils.getModelFromFile("/external_resources.ttl");
        var resource1 = model.getResource("http://www.w3.org/ns/oa#exact");
        var resource2 = model.getResource("http://www.w3.org/ns/oa#hasEndSelector");

        var indexResource1 = ResourceMapper.mapExternalToIndexResource(model, resource1);
        var indexResource2 = ResourceMapper.mapExternalToIndexResource(model, resource2);

        assertNotNull(indexResource1);
        assertNotNull(indexResource2);

        assertEquals(ResourceType.ATTRIBUTE, indexResource1.getResourceType());
        assertEquals(ResourceType.ASSOCIATION, indexResource2.getResourceType());
    }

    private Model getOrgModel(){
        var model = ModelFactory.createDefaultModel();
              model.createResource("urn:uuid:7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")
                .addProperty(RDF.type, FOAF.Organization)
                .addProperty(SKOS.prefLabel, ResourceFactory.createLangLiteral("test org", "fi"));
        return model;
    }
}
