package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.vocabulary.DCTerms;
import org.junit.jupiter.api.Test;

import javax.xml.crypto.Data;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperUtilsTest {

    @Test
    void renameResource() {
        var m = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_with_resources.ttl");

        var uri = DataModelURI.createResourceURI("test", "TestClass");
        var resource = m.getResource(uri.getResourceURI());

        assertEquals(uri.getResourceURI(), resource.getURI());
        assertEquals(uri.getResourceId(), MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));

        resource = MapperUtils.renameResource(m.getResource(uri.getResourceURI()), "NewClass");

        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "test/NewClass", resource.getURI());
        assertEquals("NewClass", MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));
    }
}