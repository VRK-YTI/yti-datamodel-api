package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MapperTestUtils {

    public static final UUID TEST_ORG_ID = UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");

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

    public static Model getMockOrganizations(){
        return getModelFromFile("/organizations.ttl");
    }

    public static Model getMockGroups(){
        return getModelFromFile("/service-categories.ttl");
    }

    public static String getJsonString(String file) throws Exception {
        return new String(OpenSearchUtils.class
                .getResourceAsStream(file).readAllBytes(), StandardCharsets.UTF_8);
    }
}
