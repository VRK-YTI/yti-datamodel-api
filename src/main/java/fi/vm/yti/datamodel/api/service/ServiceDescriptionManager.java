package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
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

    private static final Logger logger = LoggerFactory.getLogger(ServiceDescriptionManager.class.getName());

    public static final Property name = ResourceFactory.createProperty("http://www.w3.org/ns/sparql-service-description#", "name");

    private final EndpointServices endpointServices;

    ServiceDescriptionManager(EndpointServices endpointServices) {
        this.endpointServices = endpointServices;
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
            return qexec.execAsk();
        } catch (Exception ex) {
            logger.warn("Failed in checking the endpoint status: {}", endpoint);
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

        String orgString = LDHelper.concatUUIDWithReplace(orgs, ",", "<urn:uuid:@this>");
        logger.info("Parsed org UUIDs: " + orgString);

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
}
