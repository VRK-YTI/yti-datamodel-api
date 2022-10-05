package fi.vm.yti.datamodel.api.service;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
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
    private final RDFConnection coreService;
    private final RDFConnection importService;
    private final RDFConnection provService;
    private final RDFConnection schemeService;

    private final ApplicationProperties properties;

    @Autowired
    JenaClient(EndpointServices endpointServices,
               ApplicationProperties properties) {
        this.properties = properties;
        this.endpointServices = endpointServices;
        this.coreService = RDFConnection.connect(endpointServices.getCoreReadWriteAddress());
        this.importService = RDFConnection.connect(endpointServices.getImportsReadWriteAddress());
        this.provService = RDFConnection.connect(endpointServices.getProvReadWriteAddress());
        this.schemeService = RDFConnection.connect(endpointServices.getSchemesReadWriteAddress());

        if (properties.getFusekiPassword() != null && properties.getFusekiUser() != null) {
            logger.debug("Setting fuseki user & password!");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(properties.getFusekiUser(), properties.getFusekiPassword());
            credsProvider.setCredentials(AuthScope.ANY, credentials);
            HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        } else {
            logger.debug("No fuseki password found!");
        }

    }

    public Model getModelFromSchemes(String graph) {
        logger.debug("Getting model from " + graph);
        return schemeService.fetch(graph);
    }

    public void putToImports(String graph,
                             Model model) {
        logger.debug("Storing import to " + graph);
        importService.put(graph, model);
    }

    public Model getModelFromCore(String graph) {
        logger.debug("Getting model from core " + graph);
        return coreService.fetch(graph);
    }

    public Model getModelFromProv(String graph) {
        logger.debug("Getting model from prov " + graph);
        return provService.fetch(graph);
    }

    public boolean containsSchemaModel(String graph) {
        return importService.fetchDataset().containsNamedModel(graph);
    }

    public void deleteModelFromCore(String graph) {
        logger.debug("Deleting model from " + graph);
        coreService.delete(graph);
    }

    public void deleteModelFromScheme(String graph) {
        logger.debug("Deleting codelist from " + graph);
        schemeService.delete(graph);
    }

    public boolean isInCore(String graph) {
        return coreService.fetchDataset().containsNamedModel(graph);
    }

    public void putModelToCore(String graph,
                               Model model) {
        logger.debug("Putting model to " + graph);
        coreService.put(graph, model);
    }

    public void addModelToCore(String graph,
                               Model model) {
        logger.debug("Adding model to " + graph);
        coreService.load(graph, model);
    }

    public void putModelToProv(String graph,
                               Model model) {
        logger.debug("Putting to prov " + graph);
        provService.put(graph, model);
    }

    public void updateToService(UpdateRequest req,
                                String service) {
        logger.debug("Sending UpdateRequest to " + service);
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public Model constructFromService(String query,
                                      String service) {
        logger.debug("Constructing from service " + service);
        try (QueryExecution qexec = QueryExecution.service(service, query)){
            return qexec.execConstruct();
        }
    }

    public Model constructFromCore(String query) {
        logger.debug("Constructing from core " + endpointServices.getCoreSparqlAddress());
        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), query)) {
            return qexec.execConstruct();
        }
    }

    public Model constructFromExt(String query) {
        logger.debug("Constructing from ext" + endpointServices.getCoreSparqlAddress());
        try (QueryExecution qexec = QueryExecution.service(endpointServices.getImportsSparqlAddress(), query)) {
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
        try (QueryExecution qexec = QueryExecution.service(service, query)) {
            return qexec.execAsk();
        }
    }

    public ResultSet selectQuery(String service,
                                 Query query) {
        logger.debug("Select from " + service);
        try (QueryExecution qexec = QueryExecution.service(service, query)) {
            // ResultSet needs to be copied in order to use it after the connection is closed
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

    public EndpointServices getEndpointServices() {
        return this.endpointServices;
    }
}
