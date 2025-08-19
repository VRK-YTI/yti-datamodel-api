package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.endpoint.EndpointUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MapperUtilsTest {

    @Test
    void renameResource() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var uri = DataModelURI.Factory.createResourceURI("test", "TestClass");
        var resource = m.getResource(uri.getResourceURI());

        assertEquals(uri.getResourceURI(), resource.getURI());
        assertEquals(uri.getResourceId(), MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));

        resource = DataModelMapperUtils.renameResource(m.getResource(uri.getResourceURI()), "NewClass");

        assertEquals(Constants.DATA_MODEL_NAMESPACE + "test/NewClass", resource.getURI());
        assertEquals("NewClass", MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));
    }

    @Test
    void copyModel() {
        var oldGraphURI = DataModelURI.Factory.createModelURI("test", "1.0.0");
        var newGraphURI = DataModelURI.Factory.createModelURI("new_prefix");
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");
        var mockUser = EndpointUtils.mockUser;

        var copy = DataModelMapperUtils.mapCopyModel(model, mockUser, oldGraphURI, newGraphURI);

        var resources = copy.listSubjectsWithProperty(RDF.type, OWL.Class)
                .andThen(copy.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty))
                .andThen(copy.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty))
                .filterDrop(RDFNode::isAnon)
                .toList();

        var modelResource = copy.getResource(newGraphURI.getModelURI());

        var classResource = copy.getResource(DataModelURI.Factory
                .createResourceURI("new_prefix", "TestClass")
                .getResourceURI());

        var today = DateTimeFormatter
                .ofPattern("yyyy-MM-dd")
                .format(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC));

        assertEquals(model.listSubjects().toList().size(), copy.listSubjects().toList().size());

        // All version related triples should be removed
        assertFalse(modelResource.hasProperty(OWL.versionInfo));
        assertFalse(modelResource.hasProperty(OWL2.versionIRI));

        // Datamodel name should have suffix (Copy) in each language
        assertTrue(
                MapperUtils.localizedPropertyToMap(modelResource, RDFS.label)
                        .values().stream()
                        .allMatch(label -> label.endsWith((" (Copy)"))));

        // New namespace and prefix
        assertEquals("https://iri.suomi.fi/model/new_prefix/",
                MapperUtils.propertyToString(modelResource, DCAP.preferredXMLNamespace));
        assertEquals("new_prefix", MapperUtils.propertyToString(modelResource, DCAP.preferredXMLNamespacePrefix));

        // Objects (e.g. rdfs:subClassOf) should point to URIs with new prefix
        assertEquals("https://iri.suomi.fi/model/new_prefix/SubClass", MapperUtils.propertyToString(classResource, RDFS.subClassOf));

        // Update metadata
        assertTrue(resources.stream().allMatch(r -> {
            var validURI = r.getURI().startsWith(newGraphURI.getGraphURI());
            var validStatus = MapperUtils.getStatusUri(Status.DRAFT).equals(MapperUtils.propertyToString(r, SuomiMeta.publicationStatus));
            var validCreator = mockUser.getId().toString().equals(MapperUtils.propertyToString(r, SuomiMeta.creator));
            var validModifier = mockUser.getId().toString().equals(MapperUtils.propertyToString(r, SuomiMeta.modifier));

            var created = MapperUtils.propertyToString(r, DCTerms.created);
            var modified = MapperUtils.propertyToString(r, DCTerms.modified);
            var validCreated = created != null && created.startsWith(today);
            var validModified = modified != null && modified.startsWith(today);

            return validURI && validStatus && validCreator && validModifier && validCreated && validModified;
        }));
    }
}