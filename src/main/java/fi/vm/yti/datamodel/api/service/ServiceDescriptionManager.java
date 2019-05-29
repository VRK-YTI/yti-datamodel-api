package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Service;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ServiceDescriptionManager {

    static final private Logger logger = LoggerFactory.getLogger(ServiceDescriptionManager.class.getName());

    public static final Property name = ResourceFactory.createProperty("http://www.w3.org/ns/sparql-service-description#", "name");
    public static final Resource NamedGraph = ResourceFactory.createResource("http://www.w3.org/ns/sparql-service-description#NamedGraph");

    private final EndpointServices endpointServices;

    ServiceDescriptionManager(EndpointServices endpointServices) {
        this.endpointServices = endpointServices;
    }

    /**
     * Updates modified time to service description
     *
     * @param graph graph of the model
     */
    public void updateGraphDescription(String graph) {

        Literal timestamp = LDHelper.getDateTimeLiteral();

        String query =
            "WITH <urn:csc:iow:sd>" +
                "DELETE { " +
                " ?graph dcterms:modified ?date . " +
                "} " +
                "INSERT { " +
                " ?graph dcterms:modified ?timestamp " +
                "} WHERE {" +
                " ?service a sd:Service . " +
                " ?service sd:graphCollection ?graphCollection . " +
                " ?graphCollection sd:namedGraph ?graph . " +
                " ?graph sd:name ?graphName . " +
                " OPTIONAL {?graph dcterms:modified ?date . }" +
                "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        pss.setLiteral("timestamp", timestamp);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();
    }

    /**
     * Get Collection of related organization UUIDs from the model
     *
     * @param model ID of the model
     * @return Collection<UUID>
     */

    public HashSet<UUID> getModelOrganizations(String model) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String getOrgs
            = "SELECT ?org WHERE { "
            + "GRAPH <urn:csc:iow:sd> { "
            + "?graph sd:name ?graphName ."
            + "?graph dcterms:contributor ?org . "
            + "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graphName", model);
        pss.setCommandText(getOrgs);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();
            HashSet<UUID> orgUUIDs = new HashSet<>();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String orgId = soln.getResource("org").toString().split("urn:uuid:")[1];
                orgUUIDs.add(UUID.fromString(orgId));
            }

            return orgUUIDs;
        }
    }

    /**
     * Checks if model is in the list of groups given
     *
     * @param model     ID of the model
     * @param groupList List of group IDs
     * @return boolean
     */
    @Deprecated
    public boolean isModelInGroup(String model,
                                  HashMap<String, Boolean> groupList) {

        Iterator<String> groupIterator = groupList.keySet().iterator();

        String groups = "";

        if (!groupList.isEmpty()) {
            groups = "VALUES ?groups { ";

            while (groupIterator.hasNext()) {
                String group = groupIterator.next();
                Node n = NodeFactory.createURI(group);
                groups = groups + " <" + n.getURI() + "> ";
            }

            groups += " }";
        }

        String queryString = " ASK { GRAPH <urn:csc:iow:sd> { " + groups + " ?graph sd:name ?graphName . ?graph dcterms:isPartOf ?groups . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("graphName", model);
        pss.setCommandText(queryString);

        String endpoint = endpointServices.getCoreSparqlAddress();

        Query query = pss.asQuery();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query)) {
            boolean b = qexec.execAsk();

            return b;

        } catch (Exception ex) {
            logger.warn("Failed in checking the endpoint status: " + endpoint);
            return false;
        }

    }

    /**
     * Creates graph description for the new model
     *
     * @param graph    ID of the graph
     * @param orgs     UUIDs of the organizations
     * @param userUUID User UUID
     */
    public void createGraphDescription(String graph,
                                       UUID userUUID,
                                       List<UUID> orgs) throws IRIException {

        Literal timestamp = LDHelper.getDateTimeLiteral();

        if (orgs == null || orgs.isEmpty()) {
            logger.warn("Cannot create graph description without organizations");
            throw new NullPointerException();
        }

        String orgString = LDHelper.concatWithReplace(orgs, ",", "<urn:uuid:@this>");
        logger.info("Parsed org UUIDs: " + orgString);

        //String serviceString = concatServices(serviceCategories);
        // logger.info("Parsed services: "+serviceString);

        String query =
            "WITH <urn:csc:iow:sd>" +
                "INSERT { ?graphCollection sd:namedGraph _:graph . " +
                " _:graph a sd:NamedGraph . " +
                " _:graph sd:name ?graphName . " +
                " _:graph dcterms:created ?timestamp . " +
                //   " _:graph dcterms:isPartOf "+serviceString+" . "+
                "_:graph dcterms:contributor " + orgString + " . " +
                " _:graph dcterms:creator ?creator . " +
                "} WHERE { " +
                " ?service a sd:Service . " +
                " ?service sd:availableGraphs ?graphCollection . " +
                " ?graphCollection a sd:GraphCollection . " +
                " FILTER NOT EXISTS { " +
                " ?graphCollection sd:namedGraph ?graph . " +
                " ?graph sd:name ?graphName . " +
                "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        if (userUUID != null) pss.setIri("creator", "urn:uuid:" + userUUID.toString());
        pss.setLiteral("timestamp", timestamp);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    /**
     * Deletes graph description
     *
     * @param graph ID of the graph
     */
    public void deleteGraphDescription(String graph) {

        String query =
            "WITH <urn:csc:iow:sd> " +
                "DELETE { " +
                " ?graphCollection sd:namedGraph ?graph . " +
                " ?graph ?p ?o " +
                "} WHERE {" +
                " ?service a sd:Service . " +
                " ?service sd:availableGraphs ?graphCollection . " +
                " ?graphCollection sd:namedGraph ?graph . " +
                " ?graph sd:name ?graphName . " +
                " ?graph ?p ?o " +
                "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graphName", graph);
        pss.setCommandText(query);

        logger.info("Removing " + graph);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();

    }

    /**
     * Renames ID of the graph in service description
     *
     * @param oldGraph Old graph ID
     * @param newGraph New graph ID
     */
    public void renameServiceGraphName(String oldGraph,
                                       String newGraph) {

        String query =
            "DELETE { " +
                "GRAPH <urn:csc:iow:sd> { " +
                "?namedGraph sd:name ?graph . " +
                "}" +
                "}" +
                "INSERT {" +
                "GRAPH <urn:csc:iow:sd> {" +
                "?namedGraph sd:name ?newIRI . " +
                "}" +
                "}" +
                "WHERE { " +
                "GRAPH <urn:csc:iow:sd> {" +
                "?graphs sd:namedGraph ?namedGraph ." +
                "?namedGraph sd:name ?graph . }" +
                //"FILTER(?graph=<http://iow.csc.fi/ap/oiliu>)"+
                //"BIND(IRI(STR('http://iow.csc.fi/ns/oiliu')) as ?newIRI) }"
                "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setIri("graph", oldGraph);
        pss.setIri("newIRI", newGraph);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointServices.getCoreSparqlUpdateAddress());
        qexec.execute();

    }
}
