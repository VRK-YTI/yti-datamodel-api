package fi.vm.yti.datamodel.api.integration;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.topbraid.shacl.vocabulary.SH;

import java.io.InputStream;
import java.util.UUID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FusekiTest {

    public static final String GRAPH_NAME = "http://uri.suomi.fi/datamodel-v2/ns/test";

    // Before running test, start yti-fuseki: docker-compose up yti-fuseki
    public static final String FUSEKI_URL = "http://localhost:3030";

    static final RDFConnection connectionWrite = RDFConnection.connect(FUSEKI_URL + "/core/data");
    static final RDFConnection connectionRead = RDFConnection.connect(FUSEKI_URL + "/core/get");
    static final RDFConnection connectionSparql = RDFConnection.connect(FUSEKI_URL + "/core/sparql");

    @BeforeClass
    public static void setUp() {
        // clean resources before test execution
        connectionWrite.delete(GRAPH_NAME);
    }

    @AfterClass
    public static void cleanResources() {
        // clean resources after test execution
        // connectionWrite.delete(GRAPH_NAME);
    }

    /**
     * Parses rdf data from file and saves datamodel metadata
     */
    @Test
    public void t001_SaveDatamodel() {
        InputStream data = this.getClass().getResourceAsStream("/create_model.json");
        Model model = ModelFactory.createDefaultModel();
        RDFParser
                .source(data)
                .lang(Lang.JSONLD)
                .parse(model);

        connectionWrite.put(GRAPH_NAME, model);
    }

    @Test
    public void testModelCreate() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        model.createResource(GRAPH_NAME)
                .addProperty(RDF.type, OWL.Ontology)
                .addProperty(RDFS.label, model.createLiteral("test", "fi"))
                .addProperty(RDFS.label, model.createLiteral("asdf", "sv"))
                .addProperty(DCTerms.language, "fi")
                .addProperty(DCTerms.language, "sv")
                .addProperty(DCTerms.isPartOf, model
                        .createResource("http://urn.fi/URN:NBN:fi:au:ptvl:v1105")
                            .addProperty(RDFS.label, model.createLiteral("Group label", "fi")));

        RDFDataMgr.write(System.out, model, Lang.JSONLD);
    }

    /**
     * Add two classes to the model
     */
    @Test
    public void t002_AddClasses() {

        Model model = connectionRead.fetch(GRAPH_NAME);

        createClassResource(model, "#TestClass",
                model.createLiteral("Test class label fi", "fi"),
                model.createLiteral("Test class label sv", "sv"));
        createClassResource(model, "#FoobarClass",
                model.createLiteral("Some label fi", "fi"));

        connectionWrite.put(GRAPH_NAME, model);
    }

    /**
     * Fetches class from model and adds new property (comment)
     */
    @Test
    public void t004_ModifyClass() {
        Model model = connectionRead.fetch(GRAPH_NAME);
        Resource resource = model.getResource(GRAPH_NAME + "#TestClass");
        resource.addProperty(RDFS.comment, model.createLiteral("Test description", "fi"));
        connectionWrite.put(GRAPH_NAME, model);
    }

    /**
     * Get class information with simple sparql query
     */
    @Test
    public void t005_GetClass() {
        String query = "CONSTRUCT WHERE {" +
                "?class ?p ?o" +
                "}";

        // with query builder
        ConstructBuilder builder = new ConstructBuilder();
        Node uri = NodeFactory.createURI(GRAPH_NAME + "#TestClass");
        Query test = builder
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("sh", "http://www.w3.org/ns/shacl#")
                .addConstruct(uri, "?p", "?o")
                .addWhere(uri, "?p", "?o")
                .build();
        System.out.println("QueryBuilder: " + test);

        ParameterizedSparqlString sparql = new ParameterizedSparqlString();
        sparql.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        sparql.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        sparql.setIri("class", GRAPH_NAME + "#TestClass");
        sparql.setCommandText(query);

        Model model = connectionSparql.queryConstruct(test);

        // RDFDataMgr.write(System.out, model, Lang.JSONLD);
    }

    /**
     * Fetch class resource from the model
     * Creates two attributes and adds them to the class
     */
    @Test
    public void t006_AddClassProperty() {
        Model model = connectionRead.fetch(GRAPH_NAME);
        Resource classResource = model.getResource(GRAPH_NAME + "#TestClass");

        String attributeId_1 = createAttributeResource(model, model
                .createLiteral("Attribute label 1", "fi"));
        String attributeId_2 = createAttributeResource(model, model
                .createLiteral("Attribute label 2", "fi"));

        classResource.addProperty(SH.property, ResourceFactory.createResource(attributeId_1));
        classResource.addProperty(SH.property, ResourceFactory.createResource(attributeId_2));

        connectionWrite.put(GRAPH_NAME, model);
    }

    @Test
    public void t998_PrintModel() {
        RDFConnection connection = RDFConnection.connect(FUSEKI_URL + "/core/get");
        Model model = connection.fetch(GRAPH_NAME);
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            // System.out.println(statement);
        }
        RDFDataMgr.write(System.out, model, Lang.JSONLD);
    }

    private void createClassResource(Model model, String classIdentifier, Literal... labels) {
        Resource resource = model.createResource(GRAPH_NAME + classIdentifier)
                .addProperty(RDF.type, SH.class_)
                .addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(GRAPH_NAME));

        for (Literal label : labels) {
            resource.addProperty(RDFS.label, label);
        }
    }

    private String createAttributeResource(Model model, Literal...labels) {
        String id = String.format("urn:uuid:%s", UUID.randomUUID());
        Resource resource = model.createResource(id)
                .addProperty(RDF.type, OWL.DatatypeProperty)
                .addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(GRAPH_NAME))
                .addProperty(SH.datatype, "string");

        for (Literal label : labels) {
            resource.addProperty(RDFS.label, label);
        }
        return id;
    }
}
