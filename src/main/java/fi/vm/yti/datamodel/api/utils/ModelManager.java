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

import org.apache.jena.rdf.model.Model;

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
    public static Model createModelFromString(String modelString) {
        Model model = ModelFactory.createDefaultModel();
        
        try {
            RDFDataMgr.read(model, new ByteArrayInputStream(modelString.getBytes("UTF-8")), Lang.JSONLD) ;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ModelManager.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        return model;
        
    }

    /**
     * Creates new model to the core database
     * @param graph Graph of the model
     * @param group Group of the model
     * @param body Content of the model as json-ld string
     * @param login User session
     * @return UUID of the model
     */
    public static UUID createNewModel(String graph, String group, String body, LoginSession login) {
        
        String service = services.getCoreReadWriteAddress();
        
        boolean success = JerseyJsonLDClient.graphIsUpdatedToTheService(graph, body, service);

        if(!success) return null;
            
        ServiceDescriptionManager.createGraphDescription(graph, group, login.getEmail());
       
        UUID provUUID = UUID.randomUUID();
                    
        /* If new model was created succesfully create prov activity */
        if(ProvenanceManager.getProvMode()) {
            ProvenanceManager.createProvenanceActivity(graph, login.getEmail(), body, provUUID);
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

    /**
     * Updates / rewrites model to the service
     * @param graph Graph to update the model
     * @param body Model as json-ld string
     * @param login Login session
     * @return UUID of the model
     */
    public static UUID updateModel(String graph, String body, LoginSession login) {
        
        String service = services.getCoreReadWriteAddress();
        ServiceDescriptionManager.updateGraphDescription(graph);

        boolean updateStatus = JerseyJsonLDClient.graphIsUpdatedToTheService(graph, body, service);
        
        if(!updateStatus) return null;
        
        GraphManager.updateModifyDates(graph);
        
        UUID provUUID = UUID.randomUUID();
                
        /* If update is successfull create new prov entity */ 
        if(ProvenanceManager.getProvMode()) {
            ProvenanceManager.createProvenanceGraph(graph, body, login.getEmail(), provUUID); 
        }

        GraphManager.createExportGraphInRunnable(graph);
        
        logger.info("Updated :"+graph);
                
        return provUUID;
    }

    /**
     * Updates model
     * @param graph Graph to be updated
     * @param model New model as jena model
     * @param login Login session
     * @return UUID of the model
     */
    
    public static UUID updateModel(String graph, Model model, LoginSession login) {
        
        String service = services.getCoreReadWriteAddress();
        ServiceDescriptionManager.updateGraphDescription(graph);

        GraphManager.putToGraph(model, graph);
        
        GraphManager.updateModifyDates(graph);
        
        UUID provUUID = UUID.randomUUID();
                
        /* If update is successfull create new prov entity */ 
        if(ProvenanceManager.getProvMode()) {
            ProvenanceManager.createProvenanceGraphFromModel(graph, model, login.getEmail(), provUUID);
        }

        GraphManager.createExportGraphInRunnable(graph);
        
        logger.info("Updated :"+graph);
                
        return provUUID;
    }
    
   

}