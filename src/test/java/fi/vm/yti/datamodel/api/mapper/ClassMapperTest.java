package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Import({
        ClassMapper.class
})
class ClassMapperTest {

    @MockBean
    JenaService jenaService;
    @MockBean
    AuthorizationManager authorizationManager;
    @Autowired
    ClassMapper mapper;

    @Test
    void testCreateClassAndMapToModel() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        ClassDTO dto = new ClassDTO();
        dto.setIdentifier("TestClass");
        dto.setSubject("http://uri.suomi.fi/terminology/test/test1");
        dto.setEquivalentClass(Set.of("http://uri.suomi.fi/datamodel/ns/int#EqClass"));
        dto.setSubClassOf(Set.of("https://www.example.com/ns/ext#SubClass"));
        dto.setEditorialNote("comment");
        dto.setLabel(Map.of("fi", "test label"));
        dto.setStatus(Status.DRAFT);
        dto.setNote(Map.of("fi", "test note"));

        mapper.createClassAndMapToModel("test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource classResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, modelResource.listProperties(DCTerms.hasPart).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#TestClass", modelResource.getProperty(DCTerms.hasPart).getString());

        assertEquals(1, classResource.listProperties(RDFS.label).toList().size());
        assertEquals("test label", classResource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", classResource.getProperty(RDFS.label).getLiteral().getLanguage());

        assertEquals(OWL.Class, classResource.getProperty(RDF.type).getResource());
        assertEquals(Status.DRAFT, Status.valueOf(classResource.getProperty(OWL.versionInfo).getObject().toString()));
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", classResource.getProperty(RDFS.isDefinedBy).getObject().toString());

        assertEquals(XSDDatatype.XSDNCName, classResource.getProperty(DCTerms.identifier).getLiteral().getDatatype());
        assertEquals("TestClass", classResource.getProperty(DCTerms.identifier).getLiteral().getString());

        assertEquals("comment", classResource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals("test note", classResource.getProperty(SKOS.note).getLiteral().getString());

        assertEquals(1, classResource.listProperties(RDFS.subClassOf).toList().size());
        assertEquals("https://www.example.com/ns/ext#SubClass", classResource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(1, classResource.listProperties(OWL.equivalentClass).toList().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#EqClass", classResource.getProperty(OWL.equivalentClass).getObject().toString());
    }

    @Test
    void testCreateClassAndMapToModelOwlThing() {
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        ClassDTO dto = new ClassDTO();
        dto.setSubClassOf(Collections.emptySet());
        dto.setStatus(Status.VALID);
        dto.setIdentifier("Identifier");

        mapper.createClassAndMapToModel("test", m, dto);

        Resource modelResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test");
        Resource classResource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#Identifier");

        assertNotNull(modelResource);
        assertNotNull(classResource);

        assertEquals(1, classResource.listProperties(RDFS.subClassOf).toList().size());
        assertEquals(OWL.Thing, classResource.getProperty(RDFS.subClassOf).getResource());
    }

    @Test
    void testMapToClassDTO(){
        when(jenaService.doesClassExistInGraph(anyString(), anyString())).thenReturn(true);
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = mapper.mapToClassDTO("test", "TestClass", m);

        // not authenticated
        assertNull(dto.getEditorialNote());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals("TestClass", dto.getIdentifier());
        assertEquals(1, dto.getEquivalentClass().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#EqClass", dto.getEquivalentClass().stream().findFirst().orElse(null));
        assertEquals(1, dto.getSubClassOf().size());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#SubClass", dto.getSubClassOf().stream().findFirst().orElse(null));
        assertEquals(1, dto.getLabel().size());
        assertEquals("test label", dto.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/test/test1", dto.getSubject());
        assertEquals(2, dto.getNote().size());
        assertEquals("test note fi", dto.getNote().get("fi"));
        assertEquals("test note en", dto.getNote().get("en"));
    }

    @Test
    void testMapToClassMinimalDTO(){
        when(jenaService.doesClassExistInGraph(anyString(), anyString())).thenReturn(true);
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/models/test_datamodel_with_minimal_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = mapper.mapToClassDTO("test", "TestClass", m);

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
    }

    @Test
    void testMapToClassDTOAuthenticatedUser() {
        when(jenaService.doesClassExistInGraph(anyString(), anyString())).thenReturn(true);
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var dto = mapper.mapToClassDTO("test", "TestClass", m);

        assertEquals("comment visible for admin", dto.getEditorialNote());
    }

    @Test
    void testMapToIndexClass(){
        when(jenaService.doesClassExistInGraph(anyString(), anyString())).thenReturn(true);
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var indexClass = mapper.mapToIndexClass(m, "http://uri.suomi.fi/datamodel/ns/test#TestClass");

        assertEquals("http://uri.suomi.fi/datamodel/ns/test#TestClass", indexClass.getId());
        assertEquals("test note fi", indexClass.getNote().get("fi"));
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestClass", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
    }

    @Test
    void testMapToIndexClassMinimal(){
        when(jenaService.doesClassExistInGraph(anyString(), anyString())).thenReturn(true);
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/models/test_datamodel_with_minimal_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);

        var indexClass = mapper.mapToIndexClass(m, "http://uri.suomi.fi/datamodel/ns/test#TestClass");

        assertEquals("http://uri.suomi.fi/datamodel/ns/test#TestClass", indexClass.getId());
        assertEquals(Status.VALID, indexClass.getStatus());
        assertEquals("TestClass", indexClass.getIdentifier());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", indexClass.getIsDefinedBy());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#", indexClass.getNamespace());
        assertEquals(1, indexClass.getLabel().size());
        assertEquals("test label", indexClass.getLabel().get("fi"));
        assertNull(indexClass.getNote());
    }

    @Test
    void testMapToUpdateClass(){
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");

        var dto = new ClassDTO();
        dto.setLabel(Map.of("fi", "new label"));
        dto.setStatus(Status.INVALID);
        dto.setNote(Map.of("fi", "new note"));
        dto.setEquivalentClass(Set.of("http://uri.suomi.fi/datamodel/ns/int#NewEq"));
        dto.setSubClassOf(Set.of("https://www.example.com/ns/ext#NewSub"));
        dto.setSubject("http://uri.suomi.fi/terminology/qwe");
        dto.setEditorialNote("new editorial note");

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#EqClass", resource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(SKOS.note).toList().size());

        mapper.mapToUpdateClass(m, "http://uri.suomi.fi/datamodel/ns/test", resource, dto);

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("new label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/qwe", resource.getProperty(DCTerms.subject).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/int#NewEq", resource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals("https://www.example.com/ns/ext#NewSub", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(Status.INVALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("new editorial note", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(1, resource.listProperties(SKOS.note).toList().size());
        assertEquals("new note", resource.getProperty(SKOS.note).getLiteral().getString());
        assertEquals("fi", resource.getProperty(SKOS.note).getLiteral().getLanguage());
    }

    @Test
    void testMapToUpdateClassNullValuesDTO(){
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");

        var dto = new ClassDTO();

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#EqClass", resource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(SKOS.note).toList().size());

        mapper.mapToUpdateClass(m, "http://uri.suomi.fi/datamodel/ns/test", resource, dto);

        assertEquals(OWL.Class, resource.getProperty(RDF.type).getResource());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test", resource.getProperty(RDFS.isDefinedBy).getObject().toString());
        assertEquals("test label", resource.getProperty(RDFS.label).getLiteral().getString());
        assertEquals("fi", resource.getProperty(RDFS.label).getLiteral().getLanguage());
        assertEquals("TestClass", resource.getProperty(DCTerms.identifier).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#EqClass", resource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals(Status.VALID.name(), resource.getProperty(OWL.versionInfo).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(SKOS.note).toList().size());
    }

    @Test
    void testMapToUpdateClassEmptyValuesDTO(){
        Model m = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/test_datamodel_with_class.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);
        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test#TestClass");

        var dto = new ClassDTO();
        dto.setSubject("");
        dto.setEquivalentClass(Collections.emptySet());
        dto.setSubClassOf(Collections.emptySet());
        dto.setEditorialNote("");
        dto.setNote(Collections.emptyMap());

        assertEquals("http://uri.suomi.fi/terminology/test/test1", resource.getProperty(DCTerms.subject).getLiteral().getString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#EqClass", resource.getProperty(OWL.equivalentClass).getObject().toString());
        assertEquals("http://uri.suomi.fi/datamodel/ns/test#SubClass", resource.getProperty(RDFS.subClassOf).getObject().toString());
        assertEquals("comment visible for admin", resource.getProperty(SKOS.editorialNote).getObject().toString());
        assertEquals(2, resource.listProperties(SKOS.note).toList().size());

        mapper.mapToUpdateClass(m, "http://uri.suomi.fi/datamodel/ns/test", resource, dto);

        assertNull(resource.getProperty(DCTerms.subject));
        assertNull(resource.getProperty(OWL.equivalentClass));
        //OWl thing is default value if all subClassOf is emptied
        assertEquals(OWL.Thing, resource.getProperty(RDFS.subClassOf).getResource());
        assertNull(resource.getProperty(SKOS.editorialNote));
        assertNull(resource.getProperty(SKOS.note));
    }

}
