package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.glassfish.jersey.client.ClientProperties;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.InputStream;
import java.util.Iterator;

@Service
public final class TermedTerminologyManager {

    private static final Logger logger = LoggerFactory.getLogger(TermedTerminologyManager.class.getName());

    private final static Property termedId = ResourceFactory.createProperty("http://termed.thl.fi/meta/id");
    private final static Resource termedGraph = ResourceFactory.createResource("http://termed.thl.fi/meta/Graph");
    private final static Property termedGraphProperty = ResourceFactory.createProperty("http://termed.thl.fi/meta/graph");

    private final EndpointServices endpointServices;
    private final ApplicationProperties properties;
    private final ClientFactory clientFactory;
    private final NamespaceManager namespaceManager;
    private final IDManager idManager;
    private final ModelManager modelManager;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    TermedTerminologyManager(EndpointServices endpointServices,
                             ApplicationProperties properties,
                             ClientFactory clientFactory,
                             NamespaceManager namespaceManager,
                             IDManager idManager,
                             ModelManager modelManager,
                             JerseyResponseManager jerseyResponseManager) {
        this.endpointServices = endpointServices;
        this.properties = properties;
        this.clientFactory = clientFactory;
        this.namespaceManager = namespaceManager;
        this.idManager = idManager;
        this.modelManager = modelManager;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    public void initConceptsFromTermed() {
        Model schemeModel = getSchemesAsModelFromTermedAPI();
        assert schemeModel != null;
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

    public Model constructFromTempConceptService(String query ){
            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getTempConceptReadSparqlAddress(), query);
            return qexec.execConstruct();
    }

    public Model constructCleanedModelFromTempConceptService(String query) {
            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getTempConceptReadSparqlAddress(), query);
            Model objects = qexec.execConstruct();
            return cleanModelDefinitions(objects);
    }

    public Model cleanModelDefinitions(Model objects) {
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


    public Model constructCleanedModelFromTermedAPI(String conceptUri, String query) {

        Response jerseyResponse = getConceptFromTermedAPI(conceptUri);
        Model conceptModel = LDHelper.getJSONLDResponseAsJenaModel(jerseyResponse);
        QueryExecution qexec = QueryExecutionFactory.create(query,conceptModel);
        Model objects = qexec.execConstruct();
        return cleanModelDefinitions(objects);
    }

    /**
     * Returns concept as Jersey Response
     * @param uri uri of the concept
     * @return Response
     */
    public Response getConceptFromTermedAPI(String uri) {

        String url = properties.getDefaultTermedAPI()+"node-trees";

        try {
            Client client = clientFactory.createTermedClient();

            WebTarget target = client.target(url)
                    .queryParam("select", "id,uri,properties.prefLabel")
                    .queryParam("where","typeId:Concept")
                    .queryParam("where","uri:"+uri)
                    .queryParam("max", "-1");


            Response response = target.request("application/ld+json").property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE).get();

            logger.info("TERMED CALL: "+target.getUri().toString());

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info( response.getStatus()+" from URL: "+url);
                logger.info("Location: "+response.getLocation().toString());
                return jerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));

            return rb.build();
        } catch(Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return jerseyResponseManager.notAcceptable();
        }
    }

    public Model constructCleanedModelFromTermedAPIAndCore(String conceptUri, String modelUri, Query query) {

        logger.info("Constructing resource with concept: "+conceptUri);
        DatasetAccessor testAcc = DatasetAccessorFactory.createHTTP(endpointServices.getCoreReadAddress());

        Model conceptModel = searchConceptFromTermedAPIAsModel(null, null, conceptUri, null);

        assert conceptModel != null;
        conceptModel.add(testAcc.getModel(modelUri));

        QueryExecution qexec = QueryExecutionFactory.create(query,conceptModel);
        Model objects = qexec.execConstruct();

        return cleanModelDefinitions(objects);
    }

    public Model searchConceptFromTermedAPIAsModel(String query, String schemeURI, String conceptURI, String graphId) {

        String url = properties.getDefaultTermedAPI()+"node-trees";

        try {

            Client client = clientFactory.createTermedClient();

            WebTarget target = client.target(url)
                    .queryParam("select", "uri,id,references.prefLabelXl:2,properties.prefLabel,properties.definition")
                    .queryParam("where", "typeId:Concept")
                    .queryParam("max", "-1");

            if (graphId != null) {
                target = target.queryParam("where", "graphId:" + graphId);
            }

            if (conceptURI == null) {
                target = target.queryParam("where", "references.prefLabelXl.properties.prefLabel:" + LDHelper.encode(query));
            } else {

                if(conceptURI.startsWith("urn:uuid:")) conceptURI = conceptURI.replaceFirst("urn:uuid:","");

                if (idManager.isValidUrl(conceptURI)) {
                    target = target.queryParam("where", "uri:" + conceptURI);
                } else {
                    target = target.queryParam("where", "id:" + conceptURI);
                }

            }

            if (schemeURI != null && idManager.isValidUrl(schemeURI)) {
                target = target.queryParam("where", "graph.uri:" + schemeURI);
            }

            Response response = target.request("application/ld+json").get();

            Model conceptModel = LDHelper.getJSONLDResponseAsJenaModel(response);

            conceptModel.add(getSchemesAsModelFromTermedAPI());

            conceptModel = namespaceManager.renamePropertyNamespace(conceptModel, "termed:property:", "http://termed.thl.fi/meta/");

            QueryExecution qexec = QueryExecutionFactory.create(QueryLibrary.skosXlToSkos, conceptModel);

            Model simpleSkos = qexec.execConstruct();

            simpleSkos = cleanModelDefinitions(simpleSkos);
            simpleSkos.setNsPrefixes(LDHelper.PREFIX_MAP);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info( response.getStatus() + " from URL: " + url);
                return null;
            }

            return simpleSkos;
        } catch(Exception ex) {
            logger.warn(ex.getMessage());
            return null;
        }

    }


    /**
     * Returns concepts from Termed api
     * @param query query string
     *              * @param schemeURI ID of the scheme
     * @return Response
     */
    public Response searchConceptFromTermedAPI(String query, String schemeURI, String conceptURI, String graphId) {

        Model simpleSkos = searchConceptFromTermedAPIAsModel(query, schemeURI, conceptURI, graphId);

        if (simpleSkos == null) {
            return jerseyResponseManager.notFound();
        }

        ResponseBuilder rb = Response.status(Response.Status.ACCEPTED);
        rb.entity(modelManager.writeModelToJSONLDString(simpleSkos));

        return rb.build();
    }

    /**
     * Put model to concept graph
     * @param model Jena model
     * @param id IRI of the graph as String
     */
    public void putToConceptGraph(Model model, String id) {

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(endpointServices.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        try { adapter.putModel(id, model); }
        catch(NullPointerException ex) {
            logger.warn("Failed to update "+id);
        }

    }

    /**
     * Returns Jena model of the terminology
     * @return Model
     */
    public Model getSchemesAsModelFromTermedAPI() {
        String url = properties.getDefaultTermedAPI()+"node-trees";
        try {
            Client client = clientFactory.createTermedClient();

            WebTarget target = client.target(url)
                    .queryParam("select","uri,id,properties.prefLabel,properties.description")
                    .queryParam("where", "typeId:TerminologicalVocabulary")
                    .queryParam("max", "-1");

            Response response = target.request("application/rdf+xml").get();

            logger.info("TERMED CALL: "+target.getUri().toString());

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info( response.getStatus()+" from URL: "+url);
                return null;
            }

            Model schemeModel = ModelFactory.createDefaultModel();
            schemeModel.read(response.readEntity(InputStream.class), "urn:yti:terminology");

            // FIXME: This is not the way to remove arbitrary prefixes!
            schemeModel.removeNsPrefix("j.1");
            schemeModel.removeNsPrefix("j.0");
            schemeModel.removeNsPrefix("j.2");

            schemeModel = namespaceManager.renamePropertyNamespace(schemeModel, "termed:property:", "http://termed.thl.fi/meta/");

            schemeModel.setNsPrefixes(LDHelper.PREFIX_MAP);

            return schemeModel;

        } catch(Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return null;
        }
    }


    public Model getTerminologyAsJenaModel(String schemeId, String schemeUri) {

        String url = properties.getDefaultTermedAPI()+"ext";

        try {

            Client client = clientFactory.createTermedClient();

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
            logger. warn("Expect the unexpected!", ex);
            return null;
        }
    }


    /**
     * Returns concept as Jena model from temp concept graph
     * @param resourceURI
     * @return Model
     */
    public Model getConceptAsJenaModel(String resourceURI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(QueryLibrary.conceptQuery);
        pss.setIri("concept",resourceURI);
        return constructFromTempConceptService(pss.toString());
    }

    public Model getCleanedConceptAsJenaModel(String resourceURI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(QueryLibrary.conceptQuery);
        pss.setIri("concept",resourceURI);
        return constructCleanedModelFromTempConceptService(pss.toString());
    }


    public boolean isUsedConcept(String model, String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s rdfs:isDefinedBy ?model . ?s ?p ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);
        pss.setIri("model",model);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }


    public boolean isUsedConceptGlobal(String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?graph dcterms:subject ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);

        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
}
