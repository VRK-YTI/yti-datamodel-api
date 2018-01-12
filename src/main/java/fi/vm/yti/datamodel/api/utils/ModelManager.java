/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.endpoint.model.Models;
import fi.vm.yti.datamodel.api.config.EndpointServices;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import fi.vm.yti.datamodel.api.model.AbstractModel;
import org.apache.jena.rdf.model.Model;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

/**
 *
 * @author malonen
 */
public class ModelManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ModelManager.class.getName());

    /**
     * Writes jena model to string
     * @param model model to be written as json-ld string
     * @return string
     */
    public static String writeModelToString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD);
        return writer.toString();
    }


    /**
     * Create jena model from json-ld string
     * @param modelString RDF as JSON-LD string
     * @return Model
     */
    public static Model createJenaModelFromJSONLDString(String modelString) throws IllegalArgumentException {
        Model model = ModelFactory.createDefaultModel();
        
        try {
            RDFDataMgr.read(model, new ByteArrayInputStream(modelString.getBytes("UTF-8")), Lang.JSONLD) ;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ModelManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalArgumentException("Could not parse the model");
        }
       
        return model;
        
    }

    @Deprecated
    public static boolean createNewModel(String id, Model graph) {

        if (GraphManager.isExistingGraph(id)) {
            return false;
        } else {
            GraphManager.putToGraph(graph, id);
        }

        return true;
    }

    /**
     * Created new model with jena model
     * @param graph Parsed jena model
     * @param login User session
     * @return UUID of the model
     */
    @Deprecated
    public static String createNewModel(String id, Model graph, LoginSession login) {

        String provUUID = "urn:uuid:"+UUID.randomUUID();

        if (GraphManager.isExistingGraph(id)) {
            return null;
        } else {
            GraphManager.putToGraph(graph, id);

            if(ProvenanceManager.getProvMode()) {

                ProvenanceManager.createProvenanceGraphFromModel(id, graph, login.getEmail(),provUUID);
            }
        }

        return provUUID;
    }

    /**
     * Creates new model to the core database
     * @param graph Graph of the model
     * @param body Content of the model as json-ld string
     * @param login User session
     * @return UUID of the model
     */
    @Deprecated
    public static UUID createNewModel(String graph, List<UUID> orgList, String body, LoginSession login) {
        
        String service = services.getCoreReadWriteAddress();
        
        boolean success = JerseyJsonLDClient.graphIsUpdatedToTheService(graph, body, service);

        if(!success) return null;
            
        ServiceDescriptionManager.createGraphDescription(graph, login.getEmail(), orgList);
       
        UUID provUUID = UUID.randomUUID();
                    
        /* If new model was created succesfully create prov activity */
        if(ProvenanceManager.getProvMode()) {
         //   ProvenanceManager.createProvenanceActivity(graph, login.getEmail(), body, provUUID);
        }

        Logger.getLogger(Models.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
           
        try {
            GraphManager.createNewOntologyGraph(graph+"#PositionGraph");
            GraphManager.addCoreGraphToCoreGraph(graph, graph+"#ExportGraph");
        } catch(Exception ex) {
            logger.warning("Unexpected error in creating Export graph");
        }
        
        return provUUID;
    }


}