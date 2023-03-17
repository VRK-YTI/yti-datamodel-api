package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.mapper.ServiceCategoryMapper;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceCategoryMapperTest {

    @Test
    void testMapServiceCategoriesToDTO() {
        var model = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/service-categories.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, Lang.TURTLE);

        var serviceCategories = ServiceCategoryMapper.mapToListServiceCategoryDTO(model);

        assertEquals(3, serviceCategories.size());

        var cat = serviceCategories.get(0);

        assertEquals("P11", cat.getIdentifier());
        assertEquals("http://urn.fi/URN:NBN:fi:au:ptvl:v1105", cat.getId());
        assertEquals("Elinkeinot", cat.getLabel().get("fi"));
        assertEquals("Industries", cat.getLabel().get("en"));
        assertEquals("Näringar", cat.getLabel().get("sv"));
    }
}
