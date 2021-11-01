/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.StatusType;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;

@Service
public class JerseyClient {

    static final private Logger logger = LoggerFactory.getLogger(JerseyClient.class.getName());

    private final JenaClient jenaClient;
    private final EndpointServices endpointServices;
    private final ApplicationProperties properties;
    private final JerseyResponseManager jerseyResponseManager;
    private final ModelManager modelManager;
    private final ClientFactory clientFactory;

    JerseyClient(JenaClient jenaClient,
                 EndpointServices endpointServices,
                 ApplicationProperties properties,
                 JerseyResponseManager jerseyResponseManager,
                 ModelManager modelManager,
                 ClientFactory clientFactory) {
        this.jenaClient = jenaClient;
        this.endpointServices = endpointServices;
        this.properties = properties;
        this.jerseyResponseManager = jerseyResponseManager;
        this.modelManager = modelManager;
        this.clientFactory = clientFactory;
    }

    public Response getResponseFromURL(String url,
                                       String accept) {
        logger.debug("Getting " + accept + " response from " + url);
        Client client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, 180000);
        client.property(ClientProperties.READ_TIMEOUT, 180000);
        WebTarget target = client.target(url);
        Invocation.Builder requestBuilder = target.request();
        if (accept != null) requestBuilder.accept(accept);
        return requestBuilder.get();
    }

    /**
     * Returns Jersey response from Fuseki service
     *
     * @param id      Id of the graph
     * @param service Id of the service
     * @param ctype   Requested content-type
     * @return Response
     */
    public Response getResponseFromService(String id,
                                           String service,
                                           String ctype) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(service).queryParam("graph", id);
        logger.debug("Getting response from " + target.getUri().toString());
        return target.request(ctype).get();

    }


    /**
     * Returns Export graph as Jersey Response
     *
     * @param graph ID of the graph
     * @param raw   If true returns content as text
     * @param lang  Language of the required graph
     * @param ctype Required content type
     * @return Response
     */
    public Response getExportGraph(String graph,
                                   boolean raw,
                                   String lang,
                                   String ctype) {

        try {

            ContentType contentType = ContentType.create(ctype);

            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            if (rdfLang == null) {
                logger.info("Unknown RDF type: " + ctype);
                return jerseyResponseManager.notFound();
            }

            RDFFormat format = RDFWriterRegistry.defaultSerialization(rdfLang);

            Model model = jenaClient.getModelFromCore(graph + "#ExportGraph");

            ResponseBuilder rb;

            if (model != null && model.size() > 0) {
                rb = Response.ok();
            } else {
                rb = Response.noContent();
            }

            rb.entity(modelManager.writeModelToString(model, format));

            if (!raw) {
                rb.type(contentType.getContentTypeStr());
            } else {
                rb.type("text/plain");
            }

            return rb.build();

        } catch (Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return jerseyResponseManager.serverError();
        }

    }

    /**
     * Returns Jena model from the service
     *
     * @param service ID of the resource
     * @return Model
     */
    public Response getGraphsAsResponse(String service,
                                        String ctype) {

        Response response = getResponseFromURL(endpointServices.getEndpoint() + "/" + service + "/", ctype);

        logger.info(ctype + " from " + endpointServices.getEndpoint() + "/" + service + "/ response: " + response.getStatus());

        PushbackInputStream input = new PushbackInputStream(response.readEntity(InputStream.class));

        try {
            int test;
            test = input.read();
            if (test == -1) {
                logger.info(service + " is empty?");
                return Response.noContent().build();
            } else {
                input.unread(test);
                return Response.ok(input).header("Content-type", ctype).build();
            }
        } catch (IOException ex) {
            logger.info(ex.getMessage());
            return Response.noContent().build();
        }

    }

    /**
     * Returns JSON-LD Jersey response from the Fuseki service
     *
     * @param id      Id of the graph
     * @param service Id of the service
     * @return Response
     */
    public Response getGraphResponseFromService(String id,
                                                String service) {
        return getGraphResponseFromService(id, service, null);
    }

    /**
     * Returns Jersey response from the Fuseki service
     *
     * @param id      Id of the graph
     * @param service Id of the service
     * @return Response
     */
    public Response getGraphResponseFromService(String id,
                                                String service,
                                                String ctype) {

        if (ctype == null) ctype = "application/ld+json";

        Client client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, 180000);
        client.property(ClientProperties.READ_TIMEOUT, 180000);
        WebTarget target = client.target(service).queryParam("graph", id);
        Response response = target.request(ctype).get();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.info(response.getStatus() + " from SERVICE " + service + " and GRAPH " + id);
            return jerseyResponseManager.notFound();
        } else {
            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));
            return rb.build();
        }
    }

    /**
     * Returns Jersey response from Fuseki service
     *
     * @param id          Id of the graph
     * @param service     Id of the service
     * @param contentType Requested content-type
     * @param raw         boolean that states if Response is needed as raw text
     * @return Response
     */
    public Response getGraphResponseFromService(String id,
                                                String service,
                                                String contentType,
                                                boolean raw) {
        try {

            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 180000);
            client.property(ClientProperties.READ_TIMEOUT, 180000);
            WebTarget target = client.target(service).queryParam("graph", id);
            Response response = target.request(contentType).get();

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info(response.getStatus() + " from SERVICE " + service + " and GRAPH " + id);
                return jerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));

            if (!raw) {
                try {
                    rb.type(contentType);
                } catch (IllegalArgumentException ex) {
                    rb.type("text/plain;charset=utf-8");
                }
            } else {
                rb.type("text/plain;charset=utf-8");
            }

            return rb.build();
        } catch (Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }

    /**
     * Returns Jersey response from Fuseki service
     *
     * @param id          Id of the graph
     * @param service     Id of the service
     * @param contentType Requested content-type
     * @param raw         boolean that states if Response is needed as raw text
     * @return Response
     */
    public Response getNonEmptyGraphResponseFromService(String id,
                                                        String service,
                                                        String contentType,
                                                        boolean raw) {
        try {

            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 180000);
            client.property(ClientProperties.READ_TIMEOUT, 180000);
            WebTarget target = client.target(service).queryParam("graph", id);
            Response response = target.request(contentType).get();

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info(response.getStatus() + " from SERVICE " + service + " and GRAPH " + id);
                return jerseyResponseManager.okNoContent();
            }

            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));

            if (!raw) {
                try {
                    rb.type(contentType);
                } catch (IllegalArgumentException ex) {
                    rb.type("text/plain;charset=utf-8");
                }
            } else {
                rb.type("text/plain;charset=utf-8");
            }

            return rb.build();
        } catch (Exception ex) {
            logger.warn("Expect the unexpected!", ex);
            return jerseyResponseManager.unexpected();
        }
    }

    public Response saveConceptSuggestionUsingTerminologyAPI(String body,
                                                             String terminologyUri) {

        if (LDHelper.isInvalidIRI(terminologyUri)) {
            logger.warn("Invalid terminology uri in concept suggestion: " + terminologyUri);
            throw new IllegalArgumentException("Invalid terminology URI!");
        }

        String url = properties.getPrivateTerminologyAPI() + "v1/integration/terminology/conceptSuggestion";

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);

        Response response = target.request().post(Entity.entity(body, "application/json"));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.info(response.getStatus() + " from URL: " + url);
        }

        return response;
    }

    public Response constructNonEmptyGraphFromService(String query,
                                                      String service) {

        Model constructModel = jenaClient.constructFromService(query, service);

        if (constructModel.size() <= 0) {
            return jerseyResponseManager.notFound();
        }

        ResponseBuilder rb = Response.ok();
        rb.entity(modelManager.writeModelToJSONLDString(constructModel));
        return rb.build();

    }

    public void setNamespacesToModel(Model namespaceModel) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT DISTINCT ?ns ?prefix WHERE {"
            + "GRAPH ?g { "
            + "?g a owl:Ontology . "
            + "?g dcap:preferredXMLNamespaceName ?ns . "
            + "?g dcap:preferredXMLNamespacePrefix ?prefix . }}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        ResultSet results = jenaClient.selectQuery(endpointServices.getCoreSparqlAddress(), pss.asQuery());

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String namespace = soln.getLiteral("ns").getString();
            String prefix = soln.getLiteral("prefix").getString();
            namespaceModel.setNsPrefix(prefix, namespace);
        }
    }

    public Response constructGraphFromServiceWithNamespaces(String query,
                                                            String service) {

        Model constructModel = jenaClient.constructFromService(query, service);
        setNamespacesToModel(constructModel);

        if (constructModel.size() <= 0) {
            ResponseBuilder rb = Response.ok().type("application/ld+json");
            rb.entity(modelManager.writeModelToJSONLDString(constructModel));
            return rb.build();
        }

        ResponseBuilder rb = Response.ok();
        String responseString = modelManager.writeModelToJSONLDString(constructModel);
        rb.entity(responseString);
        return rb.build();
    }

    public Response constructGraphFromService(String query,
                                              String service) {

        Model constructModel = jenaClient.constructFromService(query, service);

        if (constructModel.size() <= 0) {
            ResponseBuilder rb = Response.ok().type("application/ld+json");
            rb.entity(modelManager.writeModelToJSONLDString(constructModel));
            return rb.build();
        }

        ResponseBuilder rb = Response.ok();
        String responseString = modelManager.writeModelToJSONLDString(constructModel);
        rb.entity(responseString);
        return rb.build();

    }

    /**
     * Constructs Jersey response from Jena model or returns error
     *
     * @param graph Jena model
     * @return Response
     */

    public Response constructResponseFromGraph(Model graph) {

        if (graph.size() <= 0) {
            logger.debug("Constructed graph is empty!");
        }

        ResponseBuilder rb = Response.ok().type("application/ld+json");
        rb.entity(modelManager.writeModelToJSONLDString(graph));
        return rb.build();
    }
}
