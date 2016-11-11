/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.api.model.Models;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;

/**
 *
 * @author malonen
 */
public class ModelManager {

    static EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ModelManager.class.getName());

    public static String writeModelToString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD);
        return writer.toString();
    }

    public static Model createModelFromString(String modelString) {
        Model model = ModelFactory.createDefaultModel();
        
        try {
            RDFDataMgr.read(model, new ByteArrayInputStream(modelString.getBytes("UTF-8")), Lang.JSONLD) ;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ModelManager.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        return model;
        
    }
    
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