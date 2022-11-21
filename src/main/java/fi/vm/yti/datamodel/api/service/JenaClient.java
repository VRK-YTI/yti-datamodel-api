package fi.vm.yti.datamodel.api.service;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.*;
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

    private static final Logger logger = LoggerFactory.getLogger(JenaClient.class.getName());

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
        logger.debug("Getting model from {}", graph);
        try{
            return schemeService.fetch(graph);
        }catch(HttpException ex){
            return null;
        }
    }

    public void putToImports(String graph,
                             Model model) {
        logger.debug("Storing import to {}", graph);
        importService.put(graph, model);
    }

    public Model getModelFromCore(String graph) {
        logger.debug("Getting model from core {}", graph);
        //TODO switch to query? Why does it return 404 instead of null
        try{
            return coreService.fetch(graph);
        }catch(HttpException ex){
            return null;
        }
    }

    public Model getModelFromProv(String graph) {
        logger.debug("Getting model from prov {}", graph);
        try{
            return provService.fetch(graph);
        }catch(HttpException ex){
            return null;
        }
    }

    public boolean containsSchemaModel(String graph) {
        return exists(graph, endpointServices.getImportsSparqlAddress());
    }

    public void deleteModelFromCore(String graph) {
        logger.debug("Deleting model from {}", graph);
        coreService.delete(graph);
    }

    public void deleteModelFromScheme(String graph) {
        logger.debug("Deleting codelist from {}", graph);
        schemeService.delete(graph);
    }

    public boolean isInCore(String graph) {
        return exists(graph, endpointServices.getCoreSparqlAddress());
    }

    public void putModelToCore(String graph,
                               Model model) {
        logger.debug("Putting model to {}", graph);
        coreService.put(graph, model);
    }

    public void addModelToCore(String graph,
                               Model model) {
        logger.debug("Adding model to {}", graph);
        coreService.load(graph, model);
    }

    public void putModelToProv(String graph,
                               Model model) {
        logger.debug("Putting to prov {}", graph);
        provService.put(graph, model);
    }

    public void updateToService(UpdateRequest req,
                                String service) {
        logger.debug("Sending UpdateRequest to {}", service);
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public Model constructFromService(String query,
                                      String service) {
        logger.debug("Constructing from service {}", service);
        try (QueryExecution qexec = QueryExecution.service(service, query)){
            return qexec.execConstruct();
        }
    }

    public Model constructFromCore(String query) {
        logger.debug("Constructing from core {}", endpointServices.getCoreSparqlAddress());
        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), query)) {
            return qexec.execConstruct();
        }
    }

    public Model constructFromExt(String query) {
        logger.debug("Constructing from ext {}", endpointServices.getCoreSparqlAddress());
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
        logger.debug("Asking from {}", service);
        try (QueryExecution qexec = QueryExecution.service(service, query)) {
            return qexec.execAsk();
        }
    }

    public ResultSet selectQuery(String service,
                                 Query query) {
        logger.debug("Select from {}", service);
        try (QueryExecution qexec = QueryExecution.service(service, query)) {
            // ResultSet needs to be copied in order to use it after the connection is closed
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

    public EndpointServices getEndpointServices() {
        return this.endpointServices;
    }

    private boolean exists(String uri, String endpoint) {
        String query = "ASK WHERE { GRAPH ?graph {} }";
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setIri("graph", uri);
        pss.setCommandText(query);

        boolean exists;
        try (QueryExecution exec = QueryExecution.service(endpoint, pss.toString())) {
            exists = exec.execAsk();
        }
        return exists;
    }
}
