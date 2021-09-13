package fi.vm.yti.datamodel.api.service;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;

@Service
public final class JenaClient {

    static final private Logger logger = LoggerFactory.getLogger(JenaClient.class.getName());

    private final EndpointServices endpointServices;
    private final DatasetAccessor coreService;
    private final DatasetAccessor importService;
    private final DatasetAccessor provService;
    private final DatasetAccessor schemeService;

    private final ApplicationProperties properties;

    @Autowired
    JenaClient(EndpointServices endpointServices,
               ApplicationProperties properties) {
        this.properties = properties;
        this.endpointServices = endpointServices;
        this.coreService = DatasetAccessorFactory.createHTTP(endpointServices.getCoreReadWriteAddress());
        this.importService = DatasetAccessorFactory.createHTTP(endpointServices.getImportsReadWriteAddress());
        this.provService = DatasetAccessorFactory.createHTTP(endpointServices.getProvReadWriteAddress());
        this.schemeService = DatasetAccessorFactory.createHTTP(endpointServices.getSchemesReadWriteAddress());

        if (properties.getFusekiPassword() != null && properties.getFusekiUser() != null) {
            logger.debug("Setting fuseki user & password!");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(properties.getFusekiUser(), properties.getFusekiPassword());
            credsProvider.setCredentials(AuthScope.ANY, credentials);
            HttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
            HttpOp.setDefaultHttpClient(httpclient);
        } else {
            logger.debug("No fuseki password found!");
        }

    }

    public Model getModelFromSchemes(String graph) {
        logger.debug("Getting model from " + graph);
        return schemeService.getModel(graph);
    }

    public void putToImports(String graph,
                             Model model) {
        logger.debug("Storing import to " + graph);
        importService.putModel(graph, model);
    }

    public Model getModelFromCore(String graph) {
        logger.debug("Getting model from core " + graph);
        return coreService.getModel(graph);
    }

    public Model getModelFromProv(String graph) {
        logger.debug("Getting model from prov " + graph);
        return provService.getModel(graph);
    }

    public boolean containsSchemaModel(String graph) {
        return importService.containsModel(graph);
    }

    public void deleteModelFromCore(String graph) {
        logger.debug("Deleting model from " + graph);
        coreService.deleteModel(graph);
    }

    public void deleteModelFromScheme(String graph) {
        logger.debug("Deleting codelist from " + graph);
        schemeService.deleteModel(graph);
    }

    public boolean isInCore(String graph) {
        return coreService.containsModel(graph);
    }

    public void putModelToCore(String graph,
                               Model model) {
        logger.debug("Putting model to " + graph);
        coreService.putModel(graph, model);
    }

    public void addModelToCore(String graph,
                               Model model) {
        logger.debug("Adding model to " + graph);
        coreService.add(graph, model);
    }

    public void putModelToProv(String graph,
                               Model model) {
        logger.debug("Putting to prov " + graph);
        provService.putModel(graph, model);
    }

    public void updateToService(UpdateRequest req,
                                String service) {
        logger.debug("Sending UpdateRequest to " + service);
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public Model constructFromService(String query,
                                      String service) {
        logger.debug("Constructing from " + service);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            return qexec.execConstruct();
        }
    }

    public Model constructFromCore(String query) {
        logger.debug("Constructing from " + endpointServices.getCoreSparqlAddress());
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query)) {
            return qexec.execConstruct();
        }
    }

    public Model constructFromExt(String query) {
        logger.debug("Constructing from " + endpointServices.getCoreSparqlAddress());
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getImportsSparqlAddress(), query)) {
            return qexec.execConstruct();
        }
    }

    public boolean askQuery(String service,
                            Query query,
                            String graph) {
        logger.debug("Asking from " + service + " in graph " + graph);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query, graph)) {
            return qexec.execAsk();
        }
    }

    public boolean askQuery(String service,
                            Query query) {
        logger.debug("Asking from " + service);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            return qexec.execAsk();
        }
    }

    public ResultSet selectQuery(String service,
                                 Query query) {
        logger.debug("Select from " + service);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            // ResultSet needs to be copied in order to use it after the connection is closed
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

    public EndpointServices getEndpointServices() {
        return this.endpointServices;
    }
}
