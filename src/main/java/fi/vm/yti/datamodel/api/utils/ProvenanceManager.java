/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.system.Txn;
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
    public static final Property generatedAtTime = ResourceFactory.createProperty("http://www.w3.org/ns/prov#", "generatedAtTime");


    public static boolean getProvMode() {
        return ApplicationProperties.getProvenanceMode();
    }
    
    /**
     * Put model to provenance graph
     * @param model Jena model
     * @param id IRI of the graph as String
     */
    public static void putToProvenanceGraph(Model model, String id) {
      JenaClient.putModelToProv(id, model);
    }

    /**
     * Creates Provenance activity for the given resource
     * @param id ID of the resource
     * @param model Model containing the resource
     * @param provUUID Provenance UUID for the resource
     * @param email Email of the committing user
     */
    public static void createProvenanceActivityFromModel(String id, Model model, String provUUID, String email) {
       putToProvenanceGraph(model, provUUID);
       createProvenanceActivity(id, provUUID, email);
    }

    /**
     * Returns query for creating the PROV Activity
     * @param graph ID of the resource
     * @param provUUID Provenance id of the resource
     * @param user Email of the committing user
     * @return UpdateRequest of the activity
     */

    public static UpdateRequest createProvenanceActivityRequest(String graph, String provUUID, String user) {
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
        pss.setIri("jsonld", provUUID);
        pss.setCommandText(query);
        return pss.asUpdate();
    }

    public static void createProvenanceActivity(String graph, String provUUID, String user) {
        UpdateRequest queryObj = createProvenanceActivityRequest(graph, provUUID, user);
        JenaClient.updateToService(queryObj, services.getProvSparqlUpdateAddress());
    }

    public static UpdateRequest createProvEntityRequest(String graph, String user, String provUUID) {
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
        return pss.asUpdate();
    }

    public static void createProvEntity(String graph, String provUUID, String user) {
        UpdateRequest queryObj = createProvEntityRequest(graph, user, provUUID);
        JenaClient.updateToService(queryObj, services.getProvSparqlUpdateAddress());
    }

    /**
     * Creates PROV Entities and renames ID:s if changed
     * @param graph Graph of the resource
     * @param model Model containing the resource
     * @param user Email of the committing user
     * @param provUUID Provenance UUID for the resource
     * @param oldIdIRI Optional: Old IRI for the resource
     */
    public static void createProvEntityBundle(String graph, Model model, String user, String provUUID, IRI oldIdIRI) {
      putToProvenanceGraph(model, provUUID);
      createProvEntity(graph, provUUID, user);
        if(oldIdIRI!=null) {
            ProvenanceManager.renameID(oldIdIRI.toString(), graph);
        }
    }

    /**
     * Query that renames ID:s in provenance service
     * @param oldid Old id
     * @param newid New id
     * @return
     */
    public static UpdateRequest renameIDRequest(String oldid, String newid) {

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

        return pss.asUpdate();

    }

    public static void renameID(String oldid, String newid) {
        UpdateRequest queryObj = renameIDRequest(oldid, newid);
        JenaClient.updateToService(queryObj, services.getProvSparqlUpdateAddress());
    }
    
        
    
    
}
