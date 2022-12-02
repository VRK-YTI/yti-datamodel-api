/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

@Service
public final class NamespaceManager {

    private static Logger logger = LoggerFactory.getLogger(NamespaceManager.class);

    private final EndpointServices endpointServices;
    private final JenaClient jenaClient;

    @Autowired
    NamespaceManager(EndpointServices endpointServices,
                     JenaClient jenaClient) {
        this.endpointServices = endpointServices;
        this.jenaClient = jenaClient;
    }

    /**
     * Creates model of the namespaces and tries to resolve namespaces to import service
     *
     * @return model of the namespaces
     */
    public Model getDefaultNamespaceModelAndResolve() {

        Model nsModel = ModelFactory.createDefaultModel();

        Property preferredXMLNamespaceName = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
        Property preferredXMLNamespacePrefix = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        RDFNode nsTypeStandard = ResourceFactory.createResource("http://purl.org/dc/terms/Standard");


        for(Map.Entry<String, String> ns: LDHelper.PREFIX_MAP.entrySet()){
            String prefix = ns.getKey();
            String namespace = ns.getValue();

            if (LDHelper.isPrefixResolvable(prefix)) {
                // TODO: Check & optimize resolving
                resolveNamespace(namespace, null, false);
            }

            Resource nsResource = nsModel.createResource(namespace);
            nsModel.addLiteral(nsResource, preferredXMLNamespaceName, nsModel.createLiteral(namespace));
            nsModel.addLiteral(nsResource, preferredXMLNamespacePrefix, nsModel.createLiteral(prefix));
            nsModel.addLiteral(nsResource, RDFS.label, nsModel.createLiteral(prefix, "en"));
            nsModel.add(nsResource, RDF.type, nsTypeStandard);
        }

        return nsModel;

    }

    /**
     * Tries to resolve default namespaces and saves to the fixed namespace graph
     */
    public void resolveDefaultNamespaceToTheCore() {
        jenaClient.putModelToCore("urn:csc:iow:namespaces", getDefaultNamespaceModelAndResolve());
    }

    /**
     * Creates namespace model from default prefix-map
     *
     * @return default namespace model
     */
    public Model getDefaultNamespaceModel() {

        Model nsModel = ModelFactory.createDefaultModel();

        Property preferredXMLNamespaceName = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
        Property preferredXMLNamespacePrefix = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        RDFNode nsTypeStandard = ResourceFactory.createResource("http://purl.org/dc/terms/Standard");

        for(Map.Entry<String, String> ns: LDHelper.PREFIX_MAP.entrySet()){
            String prefix = ns.getKey();
            String namespace = ns.getValue();

            Resource nsResource = nsModel.createResource(namespace);
            nsModel.addLiteral(nsResource, preferredXMLNamespaceName, nsModel.createLiteral(namespace));
            nsModel.addLiteral(nsResource, preferredXMLNamespacePrefix, nsModel.createLiteral(prefix));
            nsModel.addLiteral(nsResource, RDFS.label, nsModel.createLiteral(prefix, "en"));
            nsModel.add(nsResource, RDF.type, nsTypeStandard);
        }

        return nsModel;

    }

    /**
     * Adds default namespaces to the fixed namespace graph
     */
    public void addDefaultNamespacesToCore() {
        jenaClient.putModelToCore("urn:csc:iow:namespaces", getDefaultNamespaceModel());
    }

    /**
     * Returns true if schema is already stored
     *
     * @param namespace namespace of the schema
     * @return boolean
     */
    public boolean isSchemaInStore(String namespace) {
        return jenaClient.containsSchemaModel(namespace);
    }

    /**
     * Saves model to import service
     *
     * @param namespace namespace of the schema
     * @param model     schema as jena model
     */
    public void putSchemaToStore(String namespace,
                                 Model model) {
        jenaClient.putToImports(namespace, model);
    }

    /**
     * Returns namespaces from the graph
     *
     * @param graph Graph of the model
     * @return Returns prefix-map
     */
    public Map<String, String> getCoreNamespaceMap(String graph) {

        Model model = jenaClient.getModelFromCore(graph);

        if (model == null) {
            return null;
        }

        return model.getNsPrefixMap();

    }

    /**
     * Queries and returns all prefixes and namespaces used by models
     *
     * @return Prefix map
     */
    public Map<String, String> getCoreNamespaceMap() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources
            = "SELECT ?namespace ?prefix WHERE { "
            + "GRAPH ?graph { "
            + " ?graph a ?type  "
            + " VALUES ?type { owl:Ontology dcap:DCAP }"
            + " ?graph dcap:preferredXMLNamespaceName ?namespace . "
            + " ?graph dcap:preferredXMLNamespacePrefix ?prefix . "
            + "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        Map<String, String> namespaceMap = new HashMap<>();

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
            }
        }

        return namespaceMap;

    }

    @Deprecated
    public String getExternalPredicateType(IRI predicate) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources
            = "SELECT ?type WHERE { "
            + "{ ?predicate a ?type . "
            + " VALUES ?type { owl:DatatypeProperty owl:ObjectProperty } "
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Literal ."
            + "BIND(owl:DatatypeProperty as ?type) "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Resource ."
            + "BIND(owl:ObjectProperty as ?type) "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "}UNION {"
            /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
            + "?predicate a rdf:Property . "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "?predicate rdfs:range ?rangeClass . "
            + "?rangeClass a ?rangeClassType . "
            + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "} UNION {"
            /* IF Predicate type cannot be guessed */
            + "?predicate a rdf:Property . "
            + "BIND(rdf:Property as ?type)"
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Literal . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Resource . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range ?rangeClass . ?rangeClass a ?rangeClassType . }"
            + "} "
            + "}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        pss.setIri("predicate", predicate);

        String type = null;

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getImportsSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                if (soln.contains("type")) {
                    Resource resType = soln.getResource("type");
                    type = resType.getURI();
                }
            }

        }
        return type;

    }

    public boolean resolveNamespace(String namespace,
                                    String alternativeURL,
                                    boolean force) {
        String accept = "application/rdf+xml;q=1," +
                "application/turtle;q=0.8," +
                "application/x-turtle;q=0.8," +
                "text/turtle;q=0.8," +
                "application/ld+json;q=0.7," +
                "text/rdf+n3;q=0.5," +
                "application/n3;q=0.5," +
                "text/n3;q=0.5";
        return resolveNamespace(namespace, alternativeURL, force, accept);
    }

    public boolean resolveNamespace(String namespace,
                                    String alternativeURL,
                                    boolean force,
                                    String accept) {

        if(!namespace.startsWith("http") && (alternativeURL==null || alternativeURL.isEmpty() || !alternativeURL.startsWith("http"))) {
            return false;
        }

        try { // Unexpected exception

            IRI namespaceIRI = null;
            IRI alternativeIRI = null;

            try {
                IRIFactory iri = IRIFactory.iriImplementation();
                namespaceIRI = iri.construct(namespace);

                if (alternativeURL != null) {
                    alternativeIRI = iri.construct(alternativeURL);
                }

            } catch (IRIException e) {
                logger.warn("Namespace is invalid IRI!");
                return false;
            }

            if (isSchemaInStore(namespace) && !force) {
                logger.info("Schema found in store: " + namespace);
                return true;
            } else {
                logger.info("Trying to connect to: " + namespace);
                Model model = ModelFactory.createDefaultModel();

                URL url;

                try {
                    if (alternativeIRI != null) {
                        url = new URL(alternativeURL);
                    } else {
                        url = new URL(namespace);
                    }
                } catch (MalformedURLException e) {
                    logger.warn("Malformed Namespace URL: " + namespace);
                    return false;
                }

                if (!("https".equals(url.getProtocol()) || "http".equals(url.getProtocol()))) {
                    logger.warn("Namespace NOT http or https: " + namespace);
                    return false;
                }

                HttpURLConnection connection;

                try { // IOException

                    connection = (HttpURLConnection) url.openConnection();
                    // 2,5 seconds
                    connection.setConnectTimeout(8000);
                    // 2,5 minutes
                    connection.setReadTimeout(30000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("Accept", accept);

                    try { // SocketTimeOut

                        connection.connect();

                        InputStream stream;

                        try {
                            stream = connection.getInputStream();
                        } catch (IOException e) {
                            try {
                                // Try fallback to rdf/xml or turtle without q factor
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setConnectTimeout(8000);
                                connection.setReadTimeout(30000);
                                connection.setInstanceFollowRedirects(true);
                                connection.setRequestProperty("Accept", "application/rdf+xml,application/turtle,text/turtle,application/ld+json");
                                stream = connection.getInputStream();
                            } catch (IOException ex) {
                                logger.warn(ex.getMessage());
                                logger.warn("Couldnt read from " + namespace);
                                return false;
                            }
                        }

                        String resolvedUrl = connection.getURL().toString();
                        logger.info("Opened connection");
                        logger.info("Resolved URL: " + resolvedUrl);
                        logger.info("Content-Type: " + connection.getContentType());

                        if (connection.getContentType() == null) {
                            logger.info("Couldnt resolve Content-Type from: " + namespace);
                            return false;
                        }

                        String contentType = connection.getContentType();

                        if (contentType == null) {
                            logger.info("ContentType is null");
                            stream.close();
                            connection.disconnect();
                            return false;
                        }

                        ContentType guess = ContentType.create(contentType);
                        Lang testLang = RDFLanguages.contentTypeToLang(guess);

                        if (contentType.equals("application/xml") || resolvedUrl.endsWith(".xml") || resolvedUrl.endsWith(".rdf")) {
                            // Try parsing as rdf/xml
                            testLang = RDFLanguages.fileExtToLang("rdf");
                        } else if (resolvedUrl.endsWith(".ttl")) {
                            testLang = RDFLanguages.fileExtToLang("ttl");
                        } else if (resolvedUrl.endsWith(".nt")) {
                            testLang = RDFLanguages.fileExtToLang("nt");
                        } else if (resolvedUrl.endsWith(".jsonld")) {
                            testLang = RDFLanguages.fileExtToLang("jsonld");
                        }

                        if (testLang != null) {

                            logger.info("Trying to parse " + testLang.getName() + " from " + namespace);


                            RDFReaderI reader = model.getReader(testLang.getName());

                            reader.setProperty("error-mode", "lax");

                            try {
                                logger.info("" + stream.available());
                                reader.read(model, stream, namespace);
                            } catch (RiotException e) {
                                logger.info("Could not read file from " + namespace);
                                return false;
                            }

                            stream.close();
                            connection.disconnect();

                        } else {
                            logger.info("Could not parse RDF format from content-type!");
                            try {
                                RDFParser.create()
                                        .lang(Lang.JSONLD)
                                        .source(stream)
                                        .parse(model);
                                logger.info("Parsed something out of " + contentType + " from " + resolvedUrl);
                            } catch (Exception e) {
                                logger.error("Failed to parse RDF using " + contentType + " from " + resolvedUrl, e);
                                return false;
                            }

                            stream.close();
                            connection.disconnect();
                        }

                    } catch (UnknownHostException e) {
                        logger.warn("Invalid hostname " + namespace);
                        return false;
                    } catch (SocketTimeoutException e) {
                        logger.info("Timeout from " + namespace);
                        logger.warn(e.getMessage(), e);
                        return false;
                    } catch (RuntimeIOException e) {
                        logger.info("Could not parse " + namespace);
                        logger.warn(e.getMessage(), e);
                        return false;
                    }

                } catch (IOException e) {
                    logger.info("Could not read file from " + namespace);
                    return false;
                }

                logger.info("Model-size is: " + model.size());

                try {
                    if (model.size() > 1) {
                        putSchemaToStore(namespace, model);
                    } else {
                        logger.warn("Namespace contains empty schema: " + namespace);
                        return false;
                    }

                    return true;

                } catch (HttpException ex) {
                    logger.warn("Error in saving the model loaded from " + namespace);
                    return false;
                }

            }

        } catch (Exception ex) {
            logger.warn("Error in loading the " + namespace);
            logger.warn(ex.getMessage(), ex);
            return false;
        }
    }
}
