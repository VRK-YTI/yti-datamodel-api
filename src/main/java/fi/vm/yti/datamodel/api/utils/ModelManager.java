/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.endpoint.model.Models;
import fi.vm.yti.datamodel.api.config.EndpointServices;

import java.io.*;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

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
    public static String writeModelToJSONLDString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD);
        return writer.toString();
    }

    public static String writeModelToString(Model model, RDFFormat format) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, format);
        return writer.toString();
    }

    public static String writeModelToJSONLDString(Model m, Context jenaContext) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
            DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
            PrefixMap pm = RiotLib.prefixMap(g);
            //PrefixMap pm = PrefixMapFactory.create(m.getNsPrefixMap());
            String base = null;
            w.write(out, g, pm, base, jenaContext) ;
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    public static Model removeListStatements(Model resource, Model model) {

        StmtIterator listIterator = resource.listStatements();

        // OMG: Iterate trough all RDFLists and remove old lists from exportModel
        while(listIterator.hasNext()) {
            Statement listStatement = listIterator.next();
            if(!listStatement.getSubject().isAnon() && listStatement.getObject().isAnon()) {
                Statement removeStatement = model.getRequiredProperty(listStatement.getSubject(), listStatement.getPredicate());
                RDFList languageList = removeStatement.getObject().as(RDFList.class);
                languageList.removeList();
                removeStatement.remove();

            }
        }

        return model;

    }

    public static Model removeResourceStatements(Model resource, Model model) {

        StmtIterator listIterator = resource.listStatements();

        while(listIterator.hasNext()) {
            Statement listStatement = listIterator.next();
            Resource subject = listStatement.getSubject();
            RDFNode object = listStatement.getObject();
            if(subject.isURIResource() && object.isAnon()) {
                try {
                    Statement removeStatement = model.getRequiredProperty(subject, listStatement.getPredicate());
                    if (!removeStatement.getObject().isAnon()) {
                        logger.warning("This should'nt happen!");
                        logger.warning("Bad data " + subject.toString() + "->" + listStatement.getPredicate());
                    } else {
                        RDFList languageList = removeStatement.getObject().as(RDFList.class);
                        languageList.removeList();
                        removeStatement.remove();
                    }
                } catch(PropertyNotFoundException ex) {
                    logger.warning("This should'nt happen!");
                    ex.printStackTrace();
                }
            } else if(subject.isURIResource() && !subject.hasProperty(RDF.type, OWL.Ontology)){
                model.remove(listStatement);
            }
        }

        return model;

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


}