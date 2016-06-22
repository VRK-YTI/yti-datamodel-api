/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
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
    
   

}
