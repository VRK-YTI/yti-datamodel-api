package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisualizationDataMapperTest {

    @Test
    void mapVisualizationData() {
        Model model = ModelFactory.createDefaultModel();
        Model positions = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/models/test_datamodel_visualization.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, RDFLanguages.TURTLE);

        var result = VisualizationMapper.mapVisualizationData("test", model, positions);

        assertEquals(3, result.size());

        var class1 = result.stream().filter(r -> r.getIdentifier().equals("testclass1"))
                .findFirst();
        assertTrue(class1.isPresent());

        var class2 = result.stream().filter(r -> r.getIdentifier().equals("testclass2"))
                .findFirst();
        assertTrue(class2.isPresent());

        var class3 = result.stream().filter(r -> r.getIdentifier().equals("testclass3"))
                .findFirst();
        assertTrue(class3.isPresent());

        assertEquals("Label 1", class1.get().getLabel().get("fi"));
        assertEquals("testmodel:TestClass", class1.get().getParentClasses().iterator().next());

        var parent2 = class2.get().getParentClasses().iterator().next();
        assertEquals("testclass1", parent2);

        var parent3 = class3.get().getParentClasses().iterator().next();
        assertEquals("ext:ExternalClass", parent3);

    }
}
