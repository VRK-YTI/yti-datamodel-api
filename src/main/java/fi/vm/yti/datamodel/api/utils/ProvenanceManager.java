/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

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
    
    /**
     * Put model to provenance graph
     * @param model Jena model
     * @param id IRI of the graph as String
     */
    public static void putToProvenanceGraph(Model model, String id) {
        
      DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getProvReadWriteAddress());
      DatasetAdapter adapter = new DatasetAdapter(accessor);
      
      adapter.putModel(id, model);
        
    }
    
    public static void updateVersionIdToResource(String graph, String provUUID) {
        
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
        pss.setIri("versionID", provUUID);
        pss.setCommandText(query);
        

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

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute();
       
    }
    
    public static void createProvenanceActivity(String graph, String user, String jsonld, UUID provUUID) {
        
        createVersionIdToResource(graph, provUUID);
        
        StatusType status = JerseyJsonLDClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());
        
        if (status.getFamily() == Response.Status.Family.SUCCESSFUL) {
            
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
            logger.info(status.getFamily().toString());
            logger.info(status.toString());
            logger.warning("Failed in creating PROV graph from "+graph);
            /* TODO: Else failed entity? */
        }

    }
    
    public static void createProvenanceGraph(String graph, String jsonld, String user, String provUUID) {
        //createProvenanceGraphInRunnable(graph,jsonld,user,provUUID);
        ThreadExecutor.pool.execute(new ProvenanceGraphRunnable(graph, jsonld, user, provUUID));
    }
    
    public static void createProvenanceGraphInRunnable(String graph, String jsonld, String user, String provUUID) {
        
        logger.info("Creating prov graph "+graph+" "+provUUID.toString());
        StatusType status = JerseyJsonLDClient.putGraphToTheService(provUUID, jsonld, services.getProvReadWriteAddress());
        
        if (status.getFamily() == Response.Status.Family.SUCCESSFUL) {
            createProvEntity(graph, provUUID, user);
        } else {
            logger.info(status.getFamily().toString());
            logger.info(status.toString());
            logger.warning("Failed in creating PROV graph from "+graph);
            /* TODO: Else failed entity? */
        }
    }
    
    public static void createProvenanceGraphFromModel(String graph, Model model, String user, String provUUID) {
        putToProvenanceGraph(model,provUUID);
        createProvEntity(graph, provUUID, user);
    }
    
    public static void createProvEntity(String graph, String provUUID, String user) {
        
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
        pss.setIri("jsonld", provUUID);
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
    }
    
    public static void renameID(String oldid, String newid) {
         
            String query
                = "INSERT { "
                + "GRAPH ?newid { "
                + "?newid prov:generated ?jsonld . "
                + "?newid prov:startedAtTime ?creation . "
                + "?newid prov:used ?any . "
                + "?newid a prov:Activity . "
                + "?newid prov:wasAttributedTo ?user . "                
                + "?jsonld a prov:Entity . "
                + "?jsonld prov:wasAttributedTo ?user . "
                + "?jsonld prov:generatedAtTime ?creation . "
                + "?jsonld prov:wasRevisionOf ?oldEntity . " 
                + "}}"
                + "WHERE { "
                + "GRAPH ?oldid { "
                + "?oldid prov:startedAtTime ?creation . "
                + "?oldid prov:generated ?jsonld . "
                + "?oldid prov:used ?any . "
                + "?oldid a prov:Activity . "
                + "?oldid prov:wasAttributedTo ?user . "
                + "?jsonld a prov:Entity . "
                + "?jsonld prov:wasAttributedTo ?user . "
                + "?jsonld prov:generatedAtTime ?creation . "
                + "OPTIONAL {?jsonld prov:wasRevisionOf ?oldEntity . }"
                + "}"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        pss.setIri("oldid", oldid);
        pss.setIri("newid", newid);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getProvSparqlUpdateAddress());
        qexec.execute();
        
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getProvReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        adapter.deleteModel(oldid);
        
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
