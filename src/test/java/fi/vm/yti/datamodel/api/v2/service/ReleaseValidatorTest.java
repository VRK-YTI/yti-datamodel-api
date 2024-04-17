package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.validator.release.LibraryReferenceValidator;
import fi.vm.yti.datamodel.api.v2.validator.release.ReferencesExistsValidator;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        ReferencesExistsValidator.class,
})
class ReleaseValidatorTest {

    @MockBean
    CoreRepository coreRepository;

    @Autowired
    ReferencesExistsValidator referencesExistsValidator;

    @Test
    void testExternalReferencesExists() {
        var model = MapperTestUtils.getModelFromFile("/invalid-model.ttl");

        var notExist_1 = "https://iri.suomi.fi/model/linked/1.0.0/not-exists-1";
        var notExist_2 = "https://iri.suomi.fi/model/linked/1.0.0/not-exists-2";
        var solution_1 = mockQueryResult(notExist_1);
        var solution_2 = mockQueryResult(notExist_2);

        // mock querySelect consumer, first row returns resource "notExist_1" and second one "notExist_2"
        doAnswer(a -> {
            var arg = a.getArgument(1, Consumer.class);
            arg.accept(solution_1);
            arg.accept(solution_2);
            return null;
        }).when(coreRepository).querySelect(any(Query.class), any(Consumer.class));

        var result = referencesExistsValidator.validate(model);

        assertEquals(2, result.size());

        var testClass = result.stream()
                .filter(r -> r.getResourceURI().getUri().equals("https://iri.suomi.fi/model/test-lib/test-class"))
                .findFirst();

        var testAttr = result.stream()
                .filter(r -> r.getResourceURI().getUri().equals("https://iri.suomi.fi/model/test-lib/test-attr"))
                .findFirst();

        assertTrue(testClass.isPresent());
        assertEquals("owl:onProperty", testClass.get().getProperty());
        assertEquals(notExist_2, testClass.get().getTarget());

        assertTrue(testAttr.isPresent());
        assertEquals("rdfs:subPropertyOf", testAttr.get().getProperty());
        assertEquals(notExist_1, testAttr.get().getTarget());
    }

    @Test
    void testInternalReferenceExists() {
        var model = ModelFactory.createDefaultModel();
        var dataModelURI = DataModelURI.createModelURI("test").getModelURI();
        model.setNsPrefixes(ModelConstants.PREFIXES);

        model.createResource(dataModelURI)
                .addProperty(DCAP.preferredXMLNamespace, dataModelURI);

        model.createResource(dataModelURI + "test-class")
                .addProperty(RDFS.subClassOf, ResourceFactory.createResource(dataModelURI + "not-exists"));

        var result = referencesExistsValidator.validate(model);

        assertEquals(1, result.size());

        var reference = result.iterator().next();

        assertEquals("rdfs:subClassOf", reference.getProperty());
        assertEquals(dataModelURI + "not-exists", reference.getTarget());
        assertEquals(dataModelURI + "test-class", reference.getResourceURI().getUri());
    }

    @Test
    void testLibraryReferenceValidator() {
        var model = ModelFactory.createDefaultModel();
        var dataModelURI_1 = DataModelURI.createResourceURI("test", "incomplete-1");
        var dataModelURI_2 = DataModelURI.createResourceURI("test", "incomplete-2");
        var dataModelURI_3 = DataModelURI.createResourceURI("test", "incomplete-3");

        model.createResource(dataModelURI_1.getModelURI())
                .addProperty(RDF.type, SuomiMeta.ApplicationProfile);

        model.createResource(dataModelURI_1.getResourceURI())
                .addProperty(RDFS.label, "incomplete resource 1")
                .addProperty(SH.targetClass, OWL.Thing);

        model.createResource(dataModelURI_2.getResourceURI())
                .addProperty(RDFS.label, "incomplete resource 2")
                .addProperty(SH.path, OWL2.topObjectProperty);

        model.createResource(dataModelURI_3.getResourceURI())
                .addProperty(RDFS.label, "incomplete resource 3")
                .addProperty(SH.path, OWL2.topDataProperty);

        when(coreRepository.fetch(dataModelURI_1.getGraphURI())).thenReturn(model);

        var validationResult = new LibraryReferenceValidator().validate(model);

        assertEquals(3, validationResult.size());
        var reference = validationResult.stream().filter(r -> r.getResourceURI().getUri().equals(dataModelURI_1.getResourceURI())).findFirst();

        assertTrue(reference.isPresent());
        assertEquals("incomplete resource 1", reference.get().getResourceURI().getLabel().get("en"));
        assertEquals("https://iri.suomi.fi/model/test/incomplete-1", reference.get().getResourceURI().getUri());
    }

    private QuerySolution mockQueryResult(String resourceURI) {
        var notFoundResource = ResourceFactory.createResource(resourceURI);
        var existsLiteral = ResourceFactory.createTypedLiteral(false);

        var solution = mock(QuerySolution.class);
        var rdfNode_literal = mock(RDFNode.class);
        var rdfNode_res = mock(RDFNode.class);

        when(rdfNode_literal.asLiteral()).thenReturn(existsLiteral);
        when(rdfNode_res.asResource()).thenReturn(notFoundResource);

        when(solution.get("exists")).thenReturn(rdfNode_literal);

        when(solution.get("ext_subj")).thenReturn(rdfNode_res);

        return solution;
    }
}
