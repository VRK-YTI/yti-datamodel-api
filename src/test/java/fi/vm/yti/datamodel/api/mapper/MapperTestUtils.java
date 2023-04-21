package fi.vm.yti.datamodel.api.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MapperTestUtils {


    private MapperTestUtils(){
        //private constructor
    }

    public static Model getModelFromFile(String filepath) {
        var m = ModelFactory.createDefaultModel();
        var stream = MapperTestUtils.class.getResourceAsStream(filepath);
        assertNotNull(stream);
        RDFDataMgr.read(m, stream, RDFLanguages.TURTLE);
        return m;
    }
}
