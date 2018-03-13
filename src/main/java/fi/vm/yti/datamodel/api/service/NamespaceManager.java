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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
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
import java.util.Iterator;
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
     * @return model of the namespaces
     */
    public Model getDefaultNamespaceModelAndResolve() {

        Model nsModel = ModelFactory.createDefaultModel();

        Property preferredXMLNamespaceName = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
        Property preferredXMLNamespacePrefix = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        RDFNode nsTypeStandard = ResourceFactory.createResource("http://purl.org/dc/terms/Standard");

        Iterator i = LDHelper.PREFIX_MAP.entrySet().iterator();

        while(i.hasNext()) {
            Map.Entry ns = (Map.Entry) i.next();
            String prefix = ns.getKey().toString();
            String namespace = ns.getValue().toString();

            if(LDHelper.isPrefixResolvable(prefix)) {
                // TODO: Check & optimize resolving
                resolveNamespace(namespace,null,false);
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
     * @return
     */
    public Model getDefaultNamespaceModel() {

        Model nsModel = ModelFactory.createDefaultModel();

        Property preferredXMLNamespaceName = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
        Property preferredXMLNamespacePrefix = ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");
        RDFNode nsTypeStandard = ResourceFactory.createResource("http://purl.org/dc/terms/Standard");

        Iterator i = LDHelper.PREFIX_MAP.entrySet().iterator();

        while(i.hasNext()) {
            Map.Entry ns = (Map.Entry) i.next();
            String prefix = ns.getKey().toString();
            String namespace = ns.getValue().toString();

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
     * @param namespace namespace of the schema
     * @return boolean
     */
    public boolean isSchemaInStore(String namespace) {
        return jenaClient.containsSchemaModel(namespace);
    }

    /**
     * Saves model to import service
     * @param namespace namespace of the schema
     * @param model schema as jena model
     */
    public void putSchemaToStore(String namespace, Model model) {
        jenaClient.putToImports(namespace, model);
    }

    /**
     * Returns namespaces from the graph
     * @param graph Graph of the model
     * @return Returns prefix-map
     */
    public Map<String, String> getCoreNamespaceMap(String graph) {

        Model model = jenaClient.getModelFromCore(graph);

        if(model==null) {
            return null;
        }

        return model.getNsPrefixMap();

    }

    /**
     * Copies namespaces from one graph to another
     * @param fromGraph origin graph
     * @param toGraph new graph for namespaces
     * @param fromService origin service
     * @param toService new service
     * @throws NullPointerException
     */
    public void copyNamespacesFromGraphToGraph(String fromGraph, String toGraph, String fromService, String toService) throws NullPointerException {

        /* FIXME: j.0 namespace ISSUE!? */

        DatasetAccessor fromAccessor = DatasetAccessorFactory.createHTTP(fromService);
        Model classModel = fromAccessor.getModel(fromGraph);

        if(classModel==null) {
            throw new NullPointerException();
        }

        Map<String, String> namespaces = classModel.getNsPrefixMap();

        Model copyNamespaces = ModelFactory.createDefaultModel();
        copyNamespaces.setNsPrefixes(namespaces);
        copyNamespaces.removeNsPrefix("j.0");

        DatasetAccessor toAccessor = DatasetAccessorFactory.createHTTP(toService);
        toAccessor.add(toGraph, copyNamespaces);

    }

    /**
     * Queries and returns all prefixes and namespaces used by models
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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        Map namespaceMap = new HashMap<String, String>();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getImportsSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        String type = null;

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if(soln.contains("type")) {
                Resource resType = soln.getResource("type");
                type = resType.getURI();
            }
        }

        return type;

    }

    /**
     * Renames Object namespace with replaceFirst.
     * @param model Model that needs Object nodes to be renamespaced. Uses replaceFirst to overwrite namespaces.
     * @param oldNamespace Old namespace
     * @param newNamespace New namespace
     * @return
     */
    public Model renameObjectNamespace(Model model, String oldNamespace, String newNamespace) {
        Selector definitionSelector = new SimpleSelector(null, null, (Resource) null);
        Iterator<Statement> defStatement = model.listStatements(definitionSelector).toList().iterator();

        while(defStatement.hasNext()) {
            Statement stat = defStatement.next();
            if(stat.getObject().isResource()) {
                Resource res = stat.getObject().asResource();
                String oldName = res.toString();
                if (oldName.startsWith(oldNamespace)) {
                    String newResName = oldName.replaceFirst(oldNamespace, newNamespace);
                    stat.changeObject(ResourceFactory.createResource(newResName));
                }
            }
        }
        return model;
    }

    /**
     * Renames Property namespaces with replaceFirst.
     * @param model Model that needs Property namespaces to be renamed
     * @param oldNamespace Old Property namespace
     * @param newNamespace New Property namespace
     * @return
     */
    public Model renamePropertyNamespace(Model model, String oldNamespace, String newNamespace) {
        SimpleSelector selector = new SimpleSelector();
        Iterator<Statement> defStatement = model.listStatements(selector).toList().iterator();

        while(defStatement.hasNext()) {
            Statement stat = defStatement.next();
            Property prop = stat.getPredicate();
            String oldName = prop.getURI();
            if (oldName.startsWith(oldNamespace)) {
                String newPropName = oldName.replaceFirst(oldNamespace, newNamespace);
                model.add(stat.getSubject(), ResourceFactory.createProperty(newPropName), stat.getObject());
                stat.remove();
            }
        }
        return model;
    }

    /**
     * Renames Resource namespaces with replaceFirst.
     * @param model Model that needs Resource namespace to be renamed
     * @param oldNamespace Old Resource namespace
     * @param newNamespace Old Resource namespace
     * @return
     */
    public Model renameResourceNamespace(Model model, String oldNamespace, String newNamespace) {
        Selector definitionSelector = new SimpleSelector(ResourceFactory.createResource(oldNamespace), null, (Resource) null);
        Iterator<Statement> defStatement = model.listStatements(definitionSelector).toList().iterator();

        while(defStatement.hasNext()) {
            Statement stat = defStatement.next();
            Resource res = stat.getSubject();
            String oldName = res.toString();
            if (oldName.startsWith(oldNamespace)) {
                String newResName = oldName.replaceFirst(oldNamespace, newNamespace);
                stat.remove();
                model.add(ResourceFactory.createResource(newResName), stat.getPredicate(), stat.getObject());
            }
        }
        return model;
    }
    
    /**
     * Renames namespaces in NsPrefixMap and loops trough each triple. Renames Subject and Objet namespaces with replaceFirst.
     * @param model Model that needs namespaces to be renamed
     * @param oldNamespace Old namespace
     * @param newNamespace New namespace
     * @return
     */
    public Model renameNamespace(Model model, String oldNamespace, String newNamespace) {
        // Goes trough all triples in model. Uses selector for concurrent access!
        Selector selector = new SimpleSelector();
        Iterator<Statement> tripleIterator = model.listStatements(selector).toList().iterator();

        Map<String,String> nsMap = model.getNsPrefixMap();

        for (Map.Entry<String, String> entry : nsMap.entrySet()) {
            String value = entry.getValue();
            if(value.contains(oldNamespace)) {
                entry.setValue(value.replaceFirst(oldNamespace, newNamespace));
            }
        }

        model.setNsPrefixes(nsMap);

        while(tripleIterator.hasNext()) {
            Statement triple = tripleIterator.next();
            Resource res = triple.getSubject();
            String oldName = res.toString();
            RDFNode objectNode = triple.getObject();
            // If subject has old namespace
            if (oldName.startsWith(oldNamespace)) {
                String newResName = oldName.replaceFirst(oldNamespace, newNamespace);
                if(objectNode.isResource() && !objectNode.isAnon()) {
                    String oldObjectName = triple.getObject().asResource().toString();
                    // If both subject and object has old namespace
                    if (oldObjectName.startsWith(oldNamespace)) {
                        String newObjectName = oldObjectName.replaceFirst(oldNamespace, newNamespace);
                        model.add(ResourceFactory.createResource(newResName), triple.getPredicate(), ResourceFactory.createResource(newObjectName));
                    } else {
                        // Object is some other resource
                        model.add(ResourceFactory.createResource(newResName), triple.getPredicate(), triple.getObject());
                    }
                } else {
                    // Object is literal, change only resource name
                    model.add(ResourceFactory.createResource(newResName), triple.getPredicate(), triple.getObject());
                }
                triple.remove();
            } else {
                // Subject is something else, check object
                if(objectNode.isResource() && !objectNode.isAnon()) {
                    String oldObjectName = objectNode.asResource().toString();
                    // If only object has old namespace
                    if (oldObjectName.startsWith(oldNamespace)) {
                        String newObjectName = oldObjectName.replaceFirst(oldNamespace, newNamespace);
                        triple.changeObject(ResourceFactory.createResource(newObjectName));
                    }
                }
            }
        }

        return model;
    }

    public boolean resolveNamespace(String namespace, String alternativeURL, boolean force) {

        try { // Unexpected exception

            IRI namespaceIRI = null;
            IRI alternativeIRI = null;

            try {
                IRIFactory iri = IRIFactory.iriImplementation();
                namespaceIRI = iri.construct(namespace);

                if(alternativeURL!=null) {
                    alternativeIRI = iri.construct(alternativeURL);
                }

            } catch (IRIException e) {
                logger.warn("Namespace is invalid IRI!");
                return false;
            }

            if (isSchemaInStore(namespace) && !force ) {
                logger.info("Schema found in store: "+namespace);
                return true;
            } else {
                logger.info("Trying to connect to: "+namespace);
                Model model = ModelFactory.createDefaultModel();

                URL url;

                try {
                    if(alternativeIRI!=null) {
                        url = new URL(alternativeURL);
                    } else {
                        url = new URL(namespace);
                    }
                } catch (MalformedURLException e) {
                    logger.warn("Malformed Namespace URL: "+namespace);
                    return false;
                }

                if(!("https".equals(url.getProtocol()) || "http".equals(url.getProtocol()))) {
                    logger.warn("Namespace NOT http or https: "+namespace);
                    return false;
                }

                HttpURLConnection connection = null;

                try { // IOException

                    connection = (HttpURLConnection) url.openConnection();
                    // 2,5 seconds
                    connection.setConnectTimeout(4000);
                    // 2,5 minutes
                    connection.setReadTimeout(30000);
                    connection.setInstanceFollowRedirects(true);
                    //,text/rdf+n3,application/turtle,application/rdf+n3
                    //"application/rdf+xml,application/xml,text/html");
                    connection.setRequestProperty("Accept","application/rdf+xml,application/turtle;q=0.8,application/x-turtle;q=0.8,text/turtle;q=0.8,text/rdf+n3;q=0.5,application/n3;q=0.5,text/n3;q=0.5");

                    try { // SocketTimeOut

                        connection.connect();

                        InputStream stream;

                        try {
                            stream = connection.getInputStream();
                        }  catch (IOException e) {
                            logger.warn("Couldnt read from "+namespace);
                            return false;
                        }

                        logger.info("Opened connection");
                        logger.info(connection.getURL().toString());
                        logger.info(connection.getContentType());

                        if(connection.getContentType()==null) {
                            logger.info("Couldnt resolve Content-Type from: "+namespace);
                            return false;
                        }

                        String contentType = connection.getContentType();

                        if(contentType==null){
                            logger.info("ContentType is null");
                            stream.close();
                            connection.disconnect();
                            return false;
                        }

                        ContentType guess = ContentType.create(contentType);
                        Lang testLang = RDFLanguages.contentTypeToLang(guess);

                        if(connection.getURL().toString().endsWith(".ttl"))
                            testLang = RDFLanguages.fileExtToLang("ttl");

                        if(connection.getURL().toString().endsWith(".nt"))
                            testLang = RDFLanguages.fileExtToLang("nt");

                        if(connection.getURL().toString().endsWith(".jsonld"))
                            testLang = RDFLanguages.fileExtToLang("jsonld");

                        if(testLang!=null) {

                            logger.info("Trying to parse "+testLang.getName()+" from "+namespace);

                            RDFReader reader = model.getReader(testLang.getName());

                            reader.setProperty("error-mode", "lax");

                            try {
                                logger.info(""+stream.available());
                                reader.read(model, stream, namespace);
                            } catch(RiotException e) {
                                logger.info("Could not read file from "+namespace);
                                return false;
                            }

                            stream.close();
                            connection.disconnect();

                        } else {
                            logger.info("Cound not resolve Content-Type "+contentType+" from "+namespace);
                            stream.close();
                            connection.disconnect();
                            return false;
                        }


                    } catch (UnknownHostException e) {
                        logger.warn("Invalid hostname "+namespace);
                        return false;
                    } catch (SocketTimeoutException e) {
                        logger.info("Timeout from "+namespace);
                        e.printStackTrace();
                        return false;
                    } catch (RuntimeIOException e) {
                        logger.info("Could not parse "+namespace);
                        e.printStackTrace();
                        return false;
                    }

                } catch (IOException e) {
                    logger.info("Could not read file from "+namespace);
                    return false;
                }

                logger.info("Model-size is: "+model.size());

                try {
                    if(model.size()>1) {
                        putSchemaToStore(namespace,model);
                    } else {
                        logger.warn("Namespace contains empty schema: "+namespace);
                        return false;
                    }

                    return true;

                } catch(HttpException ex) {
                    logger.warn("Error in saving the model loaded from "+namespace);
                    return false;
                }


            }

        } catch(Exception ex) {
            logger.warn("Error in loading the "+namespace);
            ex.printStackTrace();
            return false;
        }
    }
}
