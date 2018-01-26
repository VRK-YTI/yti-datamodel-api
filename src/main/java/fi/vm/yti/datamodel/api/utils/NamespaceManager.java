/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;

import fi.vm.yti.datamodel.api.endpoint.model.PredicateCreator;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.jena.iri.IRI;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.glassfish.jersey.uri.UriComponent;

/**
 *
 * @author malonen
 */
public class NamespaceManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(NamespaceManager.class.getName());

    /**
     * Creates model of the namespaces and tries to resolve namespaces to import service
     * @return model of the namespaces
     */
    public static final Model getDefaultNamespaceModelAndResolve() {
       
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
            NamespaceResolver.resolveNamespace(namespace,null,false);
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
    public static void resolveDefaultNamespaceToTheCore() {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
	    DatasetAdapter adapter = new DatasetAdapter(accessor);
	    adapter.add("urn:csc:iow:namespaces", getDefaultNamespaceModelAndResolve());
        
    }

    /**
     * Creates namespace model from default prefix-map
     * @return
     */
    public static final Model getDefaultNamespaceModel() {
       
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
    public static void addDefaultNamespacesToCore() {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
	    DatasetAdapter adapter = new DatasetAdapter(accessor);
	    adapter.putModel("urn:csc:iow:namespaces", getDefaultNamespaceModel());
        
    }

    /**
     * Returns true if schema is already stored
     * @param namespace namespace of the schema
     * @return boolean
     */
    public static boolean isSchemaInStore(String namespace) {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        return adapter.containsModel(namespace);
        
    }

    /**
     * Saves model to import service
     * @param namespace namespace of the schema
     * @param model schema as jena model
     */
    public static void putSchemaToStore(String namespace, Model model) {
        
      	DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadWriteAddress());
	    DatasetAdapter adapter = new DatasetAdapter(accessor);
	    adapter.putModel(namespace, model);
    
    }

    /**
     * Returns namespaces from the graph
     * @param graph Graph of the model
     * @param service Used service
     * @return Returns prefix-map
     */
    public static Map<String, String> getCoreNamespaceMap(String graph, String service) {
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(service);
        Model classModel = accessor.getModel(graph);
            
            if(classModel==null) {
                return null;
            }
            
            return classModel.getNsPrefixMap();
       
    }

    /**
     * Copies namespaces from one graph to another
     * @param fromGraph origin graph
     * @param toGraph new graph for namespaces
     * @param fromService origin service
     * @param toService new service
     * @throws NullPointerException
     */
    public static void copyNamespacesFromGraphToGraph(String fromGraph, String toGraph, String fromService, String toService) throws NullPointerException {
        
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
    public static Map<String, String> getCoreNamespaceMap() {

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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();
        Map namespaceMap = new HashMap<String, String>();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            namespaceMap.put(soln.getLiteral("prefix").toString(), soln.getLiteral("namespace").toString());
        }

        return namespaceMap;

    }

    @Deprecated
    public static String getExternalPredicateType(IRI predicate) {

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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getImportsSparqlAddress(), pss.asQuery());

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

    public static Model renameObjectNamespace(Model model, String oldNamespace, String newNamespace) {
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

    public static Model renamePropertyNamespace(Model model, String oldNamespace, String newNamespace) {
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

    public static Model renameResourceNamespace(Model model, String oldNamespace, String newNamespace) {
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


    public static Model renameNamespace(Model model, String oldNamespace, String newNamespace) {
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



    /**
     * Renames namespace of the model
     * @param modelID Old model id
     * @param newModelID New model id
     */
    @Deprecated
    public static void renameNamespace(String modelID, String newModelID) {

        logger.info("Changing "+modelID+" to "+newModelID);

        Model newModel = GraphManager.getCoreGraph(modelID);
        Resource modelResource = newModel.getResource(modelID);
        ResourceUtils.renameResource(modelResource,newModelID);
        GraphManager.putToGraph(newModel, modelID);
        GraphManager.removeGraph(modelID);

        ProvenanceManager.renameID(modelID, newModelID);
        ServiceDescriptionManager.renameServiceGraphName(modelID, newModelID);

        Model hasPartModel = GraphManager.getCoreGraph(modelID+"#HasPartGraph");

        NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);

        while(nodIter.hasNext()) {
            String resourceName = nodIter.nextNode().asResource().toString();
            String newResourceName = resourceName.replaceFirst(modelID, newModelID);
            logger.info("Changing "+resourceName+" to "+newResourceName);

            Model resourceModel = GraphManager.getCoreGraph(resourceName);
            Resource res = resourceModel.getResource(resourceName);
            ResourceUtils.renameResource(res,newResourceName);

            GraphManager.putToGraph(resourceModel,newResourceName);
            GraphManager.removeGraph(resourceName);
            ProvenanceManager.renameID(resourceName, newResourceName);

           }
        
    }


}
