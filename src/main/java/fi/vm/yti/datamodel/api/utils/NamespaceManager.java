/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

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

    /**
     * Renames namespace of the model
     * @param modelID Old model id
     * @param newModelID New model id
     * @param login Login session
     */
    public static void renameNamespace(String modelID, String newModelID, LoginSession login) {

        Model newModel = GraphManager.getCoreGraph(modelID);
        Resource modelResource = newModel.getResource(modelID);
        ResourceUtils.renameResource(modelResource,newModelID);
        ModelManager.updateModel(modelID, newModel , login);
        GraphManager.removeGraph(modelID);
        
        /* TODO: GET hasPartGraph and loop resources ... update with new id and update references */
        
        ProvenanceManager.renameID(modelID, newModelID);
        ServiceDescriptionManager.renameServiceGraphName(modelID, newModelID);
        
    }

    @Deprecated
    public static void renameHasPartResources(String graph, String newGraph) {
        
        /* Use Jena to change namespace? */
        
            String query = 
                  "DELETE {"
                +  "GRAPH ?hasPartGraph { "
                +  "?graph dcterms:hasPart ?resource . }"
                + "}"
                + "INSERT { "
                +  "GRAPH ?hasPartGraph { "
                +  "?graph dcterms:hasPart ?newIRI . }"
                + "} "
                +"WHERE { "
                + "GRAPH ?graph { "
                + "?graph a owl:Ontology . "
                + "} "
                + "GRAPH ?hasPartGraph { "
                + "?graph dcterms:hasPart ?resource . "
                + "FILTER(STRSTARTS(STR(?resource),STR(?graph)))"
                /* TODO : not working with / namespace ? */
                + "BIND(STRAFTER(STR(?resource), '#') AS ?localName)"
                + "BIND(IRI(CONCAT(STR(?newIRI),?localName)) as ?newIRI)}"
                + "}";
  
    }



}
