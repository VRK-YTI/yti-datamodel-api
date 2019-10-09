package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.json.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public final class TerminologyManager {

    private static final Logger logger = LoggerFactory.getLogger(TerminologyManager.class.getName());

    private final EndpointServices endpointServices;
    private final ApplicationProperties properties;
    private final ClientFactory clientFactory;
    private final NamespaceManager namespaceManager;
    private final IDManager idManager;
    private final ModelManager modelManager;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    TerminologyManager(EndpointServices endpointServices,
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

    public String createConceptSuggestionJson(String lang,
                                              String prefLabel,
                                              String definition,
                                              String user) {

        JsonObjectBuilder objBuilder = Json.createObjectBuilder();

        objBuilder.add("creator", user);
        objBuilder.add("prefLabel", Json.createObjectBuilder().add("lang", lang).add("value", prefLabel).build());
        objBuilder.add("definition", Json.createObjectBuilder().add("lang", lang).add("value", definition).build());

        return jsonObjectToPrettyString(objBuilder.build());
    }

    public String jsonObjectToPrettyString(JsonObject object) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = Json.createWriter(stringWriter);
        writer.writeObject(object);
        writer.close();
        return stringWriter.getBuffer().toString();
    }

    public Model constructFromTempConceptService(String query) {
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getTempConceptReadSparqlAddress(), query)) {
            return qexec.execConstruct();
        }
    }

    public Model constructCleanedModelFromTempConceptService(String query) {
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getTempConceptReadSparqlAddress(), query)) {
            Model objects = qexec.execConstruct();
            return cleanModelDefinitions(objects);
        }
    }

    public Model cleanModelDefinitions(Model objects) {
        Selector definitionSelector = new SimpleSelector(null, SKOS.definition, (String) null);

        Iterator<Statement> defStatement = objects.listStatements(definitionSelector).toList().iterator();

        while (defStatement.hasNext()) {
            Statement defStat = defStatement.next();
            Parser markdownParser = Parser.builder().build();
            Node defNode = markdownParser.parse(defStat.getString());
            defStat.changeObject(ResourceFactory.createLangLiteral(Jsoup.parse(HtmlRenderer.builder().build().render(defNode)).text(), defStat.getLiteral().getLanguage()));
        }

        return objects;
    }

    public Model constructModelFromTerminologyAPIAndCore(String conceptUri,
                                                           String modelUri,
                                                           Query query) {

        logger.info("Constructing resource with concept: " + conceptUri);
        DatasetAccessor testAcc = DatasetAccessorFactory.createHTTP(endpointServices.getCoreReadAddress());

        Model conceptModel = searchConceptFromTerminologyIntegrationAPIAsModel(null, null, conceptUri);

        assert conceptModel != null;
        conceptModel.add(testAcc.getModel(modelUri));

        try (QueryExecution qexec = QueryExecutionFactory.create(query, conceptModel)) {
            Model objects = qexec.execConstruct();
            return cleanModelDefinitions(objects);
        }
    }

    static private final Map<String, Object> containerContext = new LinkedHashMap<String, Object>() {
        {
            put("uri", "@id");
            put("prefLabel", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#prefLabel");
                    put("@container", "@language");
                }
            });
            put("description", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#definition");
                    put("@container", "@language");
                }
            });
            put("status", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2002/07/owl#versionInfo");
                }
            });
            put("modified", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://purl.org/dc/terms/modified");
                    put("@type", "http://www.w3.org/2001/XMLSchema#dateTime");
                }
            });
            put("language", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://purl.org/dc/terms/language");
                  //  put("@container", "@list");
                }
            });
        }
    };

    static private final Map<String, Object> resourceContext = new LinkedHashMap<String, Object>() {
        {
            put("uri", "@id");
            put("prefLabel", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#prefLabel");
                    put("@container", "@language");
                }
            });
            put("container", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#inScheme");
                    put("@type", "@id");
                }
            });
            put("description", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#definition");
                    put("@container", "@language");
                }
            });
            put("status", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2002/07/owl#versionInfo");
                }
            });
            put("modified", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://purl.org/dc/terms/modified");
                    put("@type", "http://www.w3.org/2001/XMLSchema#dateTime");
                }
            });
        }
    };

    static private final Map<String, Object> conceptContext = new LinkedHashMap<String, Object>() {
        {
            put("id", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://purl.org/dc/terms/identifier");
                }
            });
            put("prefLabel", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#prefLabel");
                    put("@container", "@language");
                }
            });
            put("definition", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#definition");
                    put("@container", "@language");
                }
            });
            put("vocabularyPrefLabel", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://purl.org/dc/terms/title");
                    put("@container", "@language");
                }
            });
            put("uri", "@id");
            put("status", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2002/07/owl#versionInfo");
                }
            });
            put("vocabularyUri", new LinkedHashMap<String, Object>() {
                {
                    put("@id", "http://www.w3.org/2004/02/skos/core#inScheme");
                    put("@type", "@id");
                }
            });
        }
    };

    public Model getSchemesModelFromTerminologyAPI(String schemeUri) {

        String url = properties.getDefaultTerminologyAPI() + "integration/containers";

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url).queryParam("includeIncomplete",true);

        if(schemeUri!=null && !schemeUri.isEmpty()) {
            target = target.queryParam("uri",schemeUri);
        }

        Response response = target.request("application/json").get();

        client.close();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.warn("Failed to connect " + response.getStatus() + ": " + url);
            return null;
        }

        Model model = LDHelper.getResultObjectResponseAsJenaModel(response, containerContext);
        String qry = LDHelper.prefix + " INSERT { ?scheme a skos:ConceptScheme . }" +
            "WHERE { ?scheme skos:prefLabel ?label . }";
        UpdateAction.parseExecute(qry, model);

        model.setNsPrefixes(LDHelper.PREFIX_MAP);

        return model;

    }

    public Model searchConceptFromTerminologyIntegrationAPIAsModel(String query, String vocabularyUri, String conceptUri) {

        if (vocabularyUri != null && vocabularyUri.isEmpty()) {
            vocabularyUri=null;
            logger.debug("Terminology uri is empty or null");
        }

        String url = properties.getDefaultTerminologyAPI() + "integration/resources";

        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url)
            .queryParam("includeIncomplete", true);

        if(conceptUri!=null && !conceptUri.isEmpty()) {
            logger.debug("Getting concept with uri: "+conceptUri);
            target = target.queryParam("uri", conceptUri);
        } else {
            if(query!=null && !query.isEmpty()) {
                logger.debug("Concept search term: " + query);
                target = target.queryParam("searchTerm", LDHelper.encode(query));
            }
        }

        if(vocabularyUri!=null && !vocabularyUri.isEmpty()) {
            logger.debug("Searching from terminology: " + vocabularyUri);
            target = target.queryParam("container", vocabularyUri);
        }

        logger.debug("Searching from ES: "+target.getUri());

        Response response = target.request("application/json").get();
        client.close();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.warn("Failed to connect " + response.getStatus() + ": " + response.getLocation());
        }

        Model model = LDHelper.getResultObjectResponseAsJenaModel(response, resourceContext);
        model.setNsPrefixes(LDHelper.PREFIX_MAP);

        String qry = LDHelper.prefix + " INSERT { ?concept a skos:Concept . }" +
            "WHERE { ?concept skos:prefLabel ?label . }";

        UpdateAction.parseExecute(qry, model);

        logger.debug("Created model of "+model.size()+" from ES");

        return model;
    }

    public Response searchConceptFromTerminologyIntegrationAPI(String query,
                                                          String vocabularyUri,
                                                          String conceptUri) {

        Model model = searchConceptFromTerminologyIntegrationAPIAsModel(query,vocabularyUri,conceptUri);

        if((vocabularyUri==null || vocabularyUri!=null && vocabularyUri.isEmpty()) && (conceptUri!=null && !conceptUri.isEmpty())) {
            vocabularyUri = model.getRequiredProperty(ResourceFactory.createResource(conceptUri),SKOS.inScheme).getResource().getURI();
        }

        Model schemesModel = getSchemesModelFromTerminologyAPI(vocabularyUri);
        model.add(schemesModel);

        String modelString = modelManager.writeModelToJSONLDString(model);

        ResponseBuilder rb = Response.status(Response.Status.OK);
        return rb.entity(modelString).build();

    }


    public Response searchConceptFromTerminologyPublicAPI(String query,
                                                          String graphId) {

        if (graphId == null || graphId != null && graphId.isEmpty()) {
            graphId = "0";
        }

        String url = properties.getDefaultTerminologyAPI() + "terminology/publicapi/searchconcept";

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url)
            .queryParam("searchTerm", LDHelper.encode(query))
            .queryParam("vocabularyId", graphId);

        Response response = target.request("application/json").get();
        client.close();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.warn("Failed to connect " + response.getStatus() + ": " + url);
            return jerseyResponseManager.serverError();
        }

        Model model = LDHelper.getJSONArrayResponseAsJenaModel(response, conceptContext);
        model.setNsPrefixes(LDHelper.PREFIX_MAP);

        /* Lift vocabulary node to separate resource */
        String qry = LDHelper.prefix + " DELETE { ?concept dcterms:title ?title . }" +
            "INSERT { ?vocabulary skos:prefLabel ?title . " +
            "?vocabulary a skos:ConceptScheme . " +
            "?concept a skos:Concept . }" +
            "WHERE { ?concept dcterms:title ?title ." +
            " ?concept skos:inScheme ?vocabulary . }";

        UpdateAction.parseExecute(qry, model);

        String modelString = modelManager.writeModelToJSONLDString(model);

        ResponseBuilder rb = Response.status(Response.Status.OK);
        return rb.entity(modelString).build();

    }

    /**
     * Put model to concept graph
     *
     * @param model Jena model
     * @param id    IRI of the graph as String
     */
    public void putToConceptGraph(Model model,
                                  String id) {

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(endpointServices.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        try {
            adapter.putModel(id, model);
        } catch (NullPointerException ex) {
            logger.warn("Failed to update " + id);
        }

    }

    public boolean isUsedConcept(String model,
                                 String concept) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = " ASK { GRAPH ?graph { ?s rdfs:isDefinedBy ?model . ?s ?p ?concept }}";
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("concept", concept);
        pss.setIri("model", model);

        Query query = pss.asQuery();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query)) {
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
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query)) {

            boolean b = qexec.execAsk();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }
}
