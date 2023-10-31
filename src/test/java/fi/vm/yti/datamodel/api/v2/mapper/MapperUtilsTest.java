package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperUtilsTest {

    @Test
    void renameResource() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var resource = m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestClass");

        assertEquals("http://uri.suomi.fi/datamodel/ns/test/TestClass", resource.getURI());
        assertEquals("TestClass", MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));

        resource = MapperUtils.renameResource(m.getResource("http://uri.suomi.fi/datamodel/ns/test/TestClass"), "NewClass");

        assertEquals("http://uri.suomi.fi/datamodel/ns/test/NewClass", resource.getURI());
        assertEquals("NewClass", MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));
    }
}