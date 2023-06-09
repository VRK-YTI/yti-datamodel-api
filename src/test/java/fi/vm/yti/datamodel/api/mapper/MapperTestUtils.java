package fi.vm.yti.datamodel.api.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

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

    public static Model getOrgModel(){
        var model = ModelFactory.createDefaultModel();
        model.createResource("urn:uuid:7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")
                .addProperty(RDF.type, FOAF.Organization)
                .addProperty(SKOS.prefLabel, ResourceFactory.createLangLiteral("test org", "fi"));
        return model;
    }
}
