/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.ClientResponse;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

/**
 *
 * @author malonen
 */
public class ProvenanceManager {
 
    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ProvenanceManager.class.getName());
    
    public static boolean getProvMode() {
        return ApplicationProperties.getProvenanceMode();
    }
    
    public static void updateVersionIdToResource(String graph, UUID provUUID) {
        
            String query
                = "DELETE { "
                + "GRAPH ?graph { "
                + "?graph dcterms:identifier ?oldVersionID . "    
                + "}} "
                + "INSERT { "
                + "GRAPH ?graph { "
                + "?graph dcterms:identifier ?versionID . "    
                + "}} "
                + "WHERE { "
                + "GRAPH ?graph { ?graph dcterms:identifier ?oldVersionID . "
                + "}"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setLiteral("versionID", "urn:uuid:"+provUUID);
        pss.setCommandText(query);
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    
    
        public static void createVersionIdToResource(String graph, UUID provUUID) {
        
            String query
                = "INSERT { "
                + "GRAPH ?graph { "
                + "?graph dcterms:identifier ?versionID . "    
                + "}}"
                + "WHERE { "
                + "GRAPH ?graph { ?graph a ?type . "
                + "}"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setLiteral("versionID", "urn:uuid:"+provUUID);
        pss.setCommandText(query);
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    public static void createProvenanceActivity(String graph, String user, String jsonld, UUID provUUID) {
        
        createVersionIdToResource(graph, provUUID);
        
        ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());
        
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            
        String query
                = "INSERT { "
                + "GRAPH ?graph { "
                + "?graph prov:startedAtTime ?creation . "
                + "?graph prov:generated ?jsonld . "
                + "?graph prov:used ?jsonld . "
                + "?graph a prov:Activity . "
                + "?graph prov:wasAttributedTo ?user . "
                + "?jsonld a prov:Entity . "
                + "?jsonld prov:wasAttributedTo ?user . "
                + "?jsonld prov:generatedAtTime ?creation . "
                + "}"
                + "GRAPH ?jsonld { "
                + "?graph a prov:Entity . "
                + "?graph dcterms:identifier ?versionID . }"
                + "}"
                + "WHERE { "
                + "BIND(now() as ?creation)"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("jsonld", "urn:uuid:"+provUUID);
        pss.setLiteral("versionID", "urn:uuid:"+provUUID);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        } else {
            logger.info(response.getStatusInfo().getFamily().toString());
            logger.info(response.getStatusInfo().toString());
            logger.warning("Failed in creating PROV graph from "+graph);
            /* TODO: Else failed entity? */
        }

    }
    
    public static void createProvenanceGraph(String graph, String jsonld, String user, UUID provUUID) {
        
        ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());
        
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            createProvEntity(graph, provUUID, user);
        } else {
            logger.info(response.getStatusInfo().getFamily().toString());
            logger.info(response.getStatusInfo().toString());
            logger.warning("Failed in creating PROV graph from "+graph);
            /* TODO: Else failed entity? */
        }
    }
    
    public static void createProvEntity(String graph, UUID provUUID, String user) {
        
        updateVersionIdToResource(graph, provUUID);
        
            String query
                = "DELETE { "
                + "GRAPH ?graph {"  
                + "?graph prov:used ?oldEntity . "
                + "}"
                + "}"
                + "INSERT { "
                + "GRAPH ?graph { "
                + "?graph prov:generated ?jsonld . "
                + "?graph prov:used ?jsonld . "
                + "?jsonld a prov:Entity . "
                + "?jsonld prov:wasAttributedTo ?user . "
                + "?jsonld prov:generatedAtTime ?creation . "
                + "?jsonld prov:wasRevisionOf ?oldEntity . " 
                + "}"
                + "GRAPH ?jsonld {"
                + "?graph a prov:Entity ."
                + "}"
                + "}"
                + "WHERE { "
                + "GRAPH ?graph { "
                + "?graph prov:used ?oldEntity . "
                + "}"    
                + "BIND(now() as ?creation)"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("jsonld", "urn:uuid:"+provUUID);
        pss.setCommandText(query);
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        
    }
    

        public static void createNewVersionModel(String model, String user, UUID provModelUUID) {
        
            
            String query
                = "INSERT {"
                + "GRAPH ?provModelUUID { "
                + "?model a prov:Entity . " 
                + "?model dcterms:hasPart ?provResourceUUID . "
                + "?model schema:version ?versionNumber . "
                + "}"
                + "GRAPH ?model { "
                + "?model dcterms:hasVersion ?provModelUUID . "
                + "?provModelUUID prov:wasDerivedFrom ?modelEntity . "
                + "?provModelUUID a prov:Entity . "
                + "?provModelUUID prov:wasAttributedTo ?user . "
                + "?provModelUUID prov:generatedAtTime ?creation . "
                + "}}"
                + "WHERE { "
                + "{SELECT (COUNT(?version)+1 AS ?versionNumber) { GRAPH ?model { ?model dcterms:hasVersion ?version . } } } "
                + "GRAPH ?model { "
                + "?model a prov:Activity . "
                + "?model prov:used ?modelEntity . "
                + "?modelEntity a prov:Entity . "
                + "}"
                + "GRAPH ?modelPartGraph { "
                + "?model dcterms:hasPart ?resource . }"
                + "GRAPH ?resource { "
                + "?resource a prov:Activity . "
                + "?resource prov:used ?provResourceUUID . "
                + "?provResourceUUID a prov:Entity . "
                + "}"
                + "GRAPH ?provResourceUUID { "
                + "?resource rdfs:isDefinedBy ?anyModel . "
                + "}" 
                + "BIND(now() as ?creation)"
                + "}";
            
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", model);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("provModelUUID", "urn:uuid:"+provModelUUID);
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        // NamespaceManager.copyNamespacesFromGraphToGraph(model,"urn:uuid:"+provModelUUID,services.getCoreReadAddress(),services.getProvReadWriteAddress());
        
        
    }
    
        
    
    
}
