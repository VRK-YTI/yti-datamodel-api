package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.commonmark.parser.Parser;
import org.commonmark.node.*;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;


/**
 * Created by malonen on 20.11.2017.
 */
public class TermedTerminologyManager {

    private static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(TermedTerminologyManager.class.getName());
    private final static Property termedId = ResourceFactory.createProperty("http://termed.thl.fi/meta/id");
    private final static Resource termedGraph = ResourceFactory.createResource("http://termed.thl.fi/meta/Graph");
    private final static Property termedGraphProperty = ResourceFactory.createProperty("http://termed.thl.fi/meta/graph");

    public static void initConceptsFromTermed() {
        Model schemeModel = getSchemesAsModelFromTermedAPI();
        putToConceptGraph(schemeModel,"urn:yti:terminology");
        Iterator<Resource> schemeList = schemeModel.listResourcesWithProperty(RDF.type, termedGraph);
        while(schemeList.hasNext()) {
            Resource scheme = schemeList.next();
            String schemeUri = scheme.toString();
            String schemeId = scheme.getProperty(termedId).getObject().toString();

            logger.info("Importing: "+schemeUri+ " "+schemeId);
            Model terminology = getTerminologyAsJenaModel(schemeId, schemeUri);
            putToConceptGraph(terminology, schemeUri);
        }
    }

    public static Model constructFromTempConceptService(String query ){
            QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getTempConceptReadSparqlAddress(), query);
            return qexec.execConstruct();
    }

    public static Model constructCleanedModelFromTempConceptService(String query) {
            QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getTempConceptReadSparqlAddress(), query);
            Model objects = qexec.execConstruct();
            return cleanModelDefinitions(objects);
    }

    public static Model cleanModelDefinitions(Model objects) {
        Selector definitionSelector = new SimpleSelector(null, SKOS.definition, (String) null);

        Iterator<Statement> defStatement = objects.listStatements(definitionSelector).toList().iterator();

        while(defStatement.hasNext()) {
            Statement defStat = defStatement.next();
            Parser markdownParser = Parser.builder().build();
            Node defNode = markdownParser.parse(defStat.getString());
            defStat.changeObject(ResourceFactory.createLangLiteral(Jsoup.parse(HtmlRenderer.builder().build().render(defNode)).text(),defStat.getLiteral().getLanguage()));
        }

        return objects;
    }


    public static Model constructCleanedModelFromTermedAPI(String conceptUri, String query) {

        Response jerseyResponse = JerseyJsonLDClient.getConceptFromTermedAPI(conceptUri);
        Model conceptModel = JerseyJsonLDClient.getJSONLDResponseAsJenaModel(jerseyResponse);
        QueryExecution qexec = QueryExecutionFactory.create(query,conceptModel);
        Model objects = qexec.execConstruct();
        return cleanModelDefinitions(objects);

    }


    public static Model constructCleanedModelFromTermedAPIAndCore(String conceptUri, String modelUri, Query query) {

        DatasetAccessor testAcc = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
        Response jerseyResponse = JerseyJsonLDClient.getConceptFromTermedAPI(conceptUri);
        Model conceptModel = JerseyJsonLDClient.getJSONLDResponseAsJenaModel(jerseyResponse);
        conceptModel.add(testAcc.getModel(modelUri));

        QueryExecution qexec = QueryExecutionFactory.create(query,conceptModel);
        Model objects = qexec.execConstruct();

        return cleanModelDefinitions(objects);

    }


    /**
     * Put model to concept graph
     * @param model Jena model
     * @param id IRI of the graph as String
     */
    public static void putToConceptGraph(Model model, String id) {

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        try { adapter.putModel(id, model); }
        catch(NullPointerException ex) {
            logger.warning("Failed to update "+id);
        }

    }

    /**
     * Returns Jena model of the terminology
     * @return Model
     */
    public static Model getSchemesAsModelFromTermedAPI() {
        String url = ApplicationProperties.getDefaultTermAPI()+"node-trees";
        try {
            Client client = JerseyJsonLDClient.IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("select","*").queryParam("where", "typeId:TerminologicalVocabulary").queryParam("max", "-1");

            Response response = target.request("application/rdf+xml").get();

            logger.info("TERMED CALL: "+target.getUri().toString());

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
                return null;
            }

            Model schemeModel = ModelFactory.createDefaultModel();
            schemeModel.read(response.readEntity(InputStream.class), "urn:yti:terminology");

            return schemeModel;

        } catch(Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return null;
        }
    }


    public static Model getTerminologyAsJenaModel(String schemeId, String schemeUri) {

        String url = ApplicationProperties.getDefaultTermAPI()+"ext";

        try {

            Client client = JerseyJsonLDClient.IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("graphId", schemeId).queryParam("max", "-1");

            Response response = target.request("application/rdf+xml").get();

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info("FAIL: "+target.getUri().toString());
                return null;
            }

            logger.info(target.getUri().toString());

            Model model = ModelFactory.createDefaultModel();

            try {
                RDFReader reader = model.getReader(Lang.RDFXML.getName());
                reader.read(model, response.readEntity(InputStream.class), schemeUri);
            } catch(RiotException ex) {
                return model;
            }

            return model;

        } catch(Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return null;
        }
    }


    /**
     * Returns concept as Jena model from temp concept graph
     * @param resourceURI
     * @return Model
     */
    public static Model getConceptAsJenaModel(String resourceURI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(QueryLibrary.conceptQuery);
        pss.setIri("concept",resourceURI);
        return constructFromTempConceptService(pss.toString());
    }

    public static Model getCleanedConceptAsJenaModel(String resourceURI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(QueryLibrary.conceptQuery);
        pss.setIri("concept",resourceURI);
        return constructCleanedModelFromTempConceptService(pss.toString());
    }


    public static boolean isUsedConcept(String model, String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s rdfs:isDefinedBy ?model . ?s ?p ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);
        pss.setIri("model",model);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }


    public static boolean isUsedConceptGlobal(String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?graph dcterms:subject ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
}
