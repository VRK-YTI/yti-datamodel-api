package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ResourceDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        ResourceMapper.class
})
class ResourceMapperTest {

    @MockBean
    JenaService jenaService;

    @Autowired
    ResourceMapper mapper;
    @Test
    void mapToResourceAssociation() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setType(ResourceType.ASSOCIATION);
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int#EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext#SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        mapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#Resource", modelResource.getProperty(DCTerms.hasPart).getString());

        assertEquals(OWL.ObjectProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(SKOS.note).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext#SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAssociationEmptySubResourceOf() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setType(ResourceType.ASSOCIATION);
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int#EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        mapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#Resource", modelResource.getProperty(DCTerms.hasPart).getString());

        assertEquals(OWL.ObjectProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(SKOS.note).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topObjectProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttributeEmptySubResourceOf() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setType(ResourceType.ATTRIBUTE);
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int#EqRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        mapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#Resource", modelResource.getProperty(DCTerms.hasPart).getString());

        assertEquals(OWL.DatatypeProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(SKOS.note).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals(OWL2.topDataProperty, resourceResource.getProperty(RDFS.subPropertyOf).getResource());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }

    @Test
    void mapToResourceAttribute() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = new ResourceDTO();
        dto.setIdentifier("Resource");
        dto.setType(ResourceType.ATTRIBUTE);
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentResource(Set.of("http://uri.suomi.fi/datamodel/ns/int#EqRes"));
        dto.setSubResourceOf(Set.of("https://www.example.com/ns/ext#SubRes"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        mapper.mapToResource("http://uri.suomi.fi/datamodel/ns/test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource resourceResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#Resource");

        assertNotNull(modelResource);
        assertNotNull(resourceResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#Resource", modelResource.getProperty(DCTerms.hasPart).getString());

        assertEquals(OWL.DatatypeProperty, resourceResource.getProperty(RDF.type).getResource());

        assertEquals(1, resourceResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", resourceResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resourceResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(Status.DRAFT, Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resourceResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, resourceResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("Resource", resourceResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", resourceResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", resourceResource.getProperty(SKOS.note).getLiteral().getString());

        assertEquals(1, resourceResource.listProperties(RDFS.subPropertyOf).toList().size());
        assertEquals("https://www.example.com/ns/ext#SubRes", resourceResource.getProperty(RDFS.subPropertyOf).getObject().toString());
        assertEquals(1, resourceResource.listProperties(OWL.equivalentProperty).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#EqRes", resourceResource.getProperty(OWL.equivalentProperty).getObject().toString());
    }
}
