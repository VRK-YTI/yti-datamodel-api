/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import static com.csc.fi.ioapi.utils.ImportManager.services;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.ClientResponse;
import java.util.Map;
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
    
    public static void createProvenanceActivity(String graph, String user, String jsonld) {
        UUID provUUID = UUID.randomUUID();
        UUID entityUUID = UUID.randomUUID();
        
        ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());
        
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            
        String query
                = "INSERT { "
                + "GRAPH ?graph { "
                + "?graph prov:startedAtTime ?creation . "
                + "?graph prov:generated ?entity . "
                + "?graph prov:used ?entity . "
                + "?graph a prov:Activity . "
                + "?graph prov:wasAttributedTo ?user . "
                + "?entity a prov:Entity . "
                + "?entity prov:wasAttributedTo ?user . "
                + "?entity prov:generatedAtTime ?creation . "
                + "?entity prov:value ?jsonld . "
                + "}}"
                + "WHERE { "
                + "BIND(now() as ?creation)"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("jsonld", "urn:uuid:"+provUUID);
        pss.setIri("entity", "urn:uuid:"+entityUUID);
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
    
    public static void createProvenanceGraph(String graph, String model, String jsonld, String user) {
        
        UUID provUUID = UUID.randomUUID();
        
        ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());
        
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            createProvEntity(graph, model, provUUID, user);
        } else {
            logger.info(response.getStatusInfo().getFamily().toString());
            logger.info(response.getStatusInfo().toString());
            logger.warning("Failed in creating PROV graph from "+graph);
            /* TODO: Else failed entity? */
        }
    }
    
    public static void createProvEntity(String graph, String model, UUID provUUID, String user) {
        
        UUID entityUUID = UUID.randomUUID();
        
            String query
                = "DELETE { "
                + "GRAPH ?graph {"
                + "?graph prov:used ?oldEntity . "
                + "}"
                + "}"
                + "INSERT { "
                + "GRAPH ?graph { "
                + "?oldEntity prov:wasInvalidatedAt ?invalidate . "    
                + "?graph prov:generated ?entity . "
                + "?graph prov:used ?entity . "    
                + "?entity a prov:Entity . "
                + "?entity prov:wasAttributedTo ?user . "
                + "?entity prov:generatedAtTime ?invalidate . "
                + "?entity prov:value ?provGraph . "
                + "?entity prov:wasRevisionOf ?oldEntity . "
                + "}}"
                + "WHERE { "
                + "GRAPH ?graph { "
                + "?graph prov:used ?oldEntity . "
                + "}"    
                + "BIND(now() as ?invalidate)"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("graph", graph);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("entity", "urn:uuid:"+entityUUID);
        pss.setIri("provGraph", "urn:uuid:"+provUUID);
        pss.setCommandText(query);
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        
    }
    

        public static UUID createNewVersionModel(String model, String user) {
        
            UUID provModelUUID = UUID.randomUUID();
            UUID newModelEntityUUID = UUID.randomUUID();
        
            String query                 
                =  "INSERT {"
                + "GRAPH ?provModelUUID { "
                + "?model dcterms:hasPart ?provResourceUUID . "
                + "?every ?darn ?thing . "
                + "}"
                + "GRAPH ?model { "
                + "?model dcterms:hasVersion ?newModelEntity . "
                + "?newModelEntity prov:wasDerivedFrom ?modelEntity . "
                + "?newModelEntity a prov:Entity . "
                + "?newModelEntity prov:wasAttributedTo ?user . "
                + "?newModelEntity prov:generatedAtTime ?creation . "
                + "?newModelEntity prov:value ?provModelUUID . "
                + "}}"
                + "WHERE { "
                + "GRAPH ?model { "
                + "?model a prov:Activity . "
                + "?model prov:used ?modelEntity . "
                + "?modelEntity a prov:Entity . "
                + "?modelEntity prov:value ?currentModel . "
                + "}"
                + "GRAPH ?currentModel { "
                + "?every ?darn ?thing . "
                + "FILTER(?darn!=<http://purl.org/dc/terms/hasPart>)"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource { "
                + "?resource a prov:Activity . "
                + "?resource prov:used ?resourceEntity . "
                + "?resourceEntity a prov:Entity . "
                + "?resourceEntity prov:value ?provResourceUUID . "
                + "}"
                + "GRAPH ?provResourceUUID { "
                + "?resource rdfs:isDefinedBy ?anyModel . "
                + "}" 
                + "BIND(now() as ?creation)"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        Map<String, String> namespaces = NamespaceManager.getCoreNamespaceMap(model, services.getProvReadWriteAddress());
        namespaces.putAll(LDHelper.PREFIX_MAP);
        
        pss.setNsPrefixes(namespaces);
        
        pss.setIri("model", model);
        pss.setIri("user", "mailto:"+user);
        pss.setIri("provModelUUID", "urn:uuid:"+provModelUUID);
        pss.setIri("newModelEntity", "urn:uuid:"+newModelEntityUUID);
        pss.setCommandText(query);
        
        logger.info(pss.toString());

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        NamespaceManager.copyNamespacesFromGraphToGraph(model,"urn:uuid:"+provModelUUID,services.getCoreReadAddress(),services.getProvReadWriteAddress());
        
        return provModelUUID;
        
    }
    
        
    
    
}
