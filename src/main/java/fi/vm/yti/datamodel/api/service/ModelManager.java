/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.io.*;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;


@Service
public class ModelManager {

    private static final Logger logger = LoggerFactory.getLogger(ModelManager.class.getName());

    /**
     * Writes jena model to string
     * @param model model to be written as json-ld string
     * @return string
     */
    public String writeModelToJSONLDString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD);
        return writer.toString();
    }

    public String writeModelToString(Model model, RDFFormat format) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, format);
        return writer.toString();
    }

    public JsonNode toJsonNode(Model m) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FLATTEN_FLAT);
        w.write(baos, g, RiotLib.prefixMap(g), null, g.getContext());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
           JsonNode jsonNode = objectMapper.readTree(baos.toByteArray());
           return jsonNode;
        } catch(IOException ex) {
           logger.warn(ex.getMessage(),ex);
           return null;
        }
    }

    public String writeModelToJSONLDString(Model m, Context jenaContext) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
            DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
            PrefixMap pm = RiotLib.prefixMap(g);
            //PrefixMap pm = PrefixMapFactory.create(m.getNsPrefixMap());
            w.write(out, g, pm, null, jenaContext) ;
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    public Model removeListStatements(Model resource, Model model) {

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

    public Model removeResourceStatements(Model resource, Model model) {

        StmtIterator listIterator = resource.listStatements();

        while(listIterator.hasNext()) {
            Statement listStatement = listIterator.next();
            Resource subject = listStatement.getSubject();
            RDFNode object = listStatement.getObject();
            if(subject.isURIResource() && object.isAnon()) {
                try {
                    Statement removeStatement = model.getRequiredProperty(subject, listStatement.getPredicate());
                    if (!removeStatement.getObject().isAnon()) {
                        logger.warn("This should'nt happen!");
                        logger.warn("Bad data " + subject.toString() + "->" + listStatement.getPredicate());
                    } else {
                        RDFList languageList = removeStatement.getObject().as(RDFList.class);
                        languageList.removeList();
                        removeStatement.remove();
                    }
                } catch(PropertyNotFoundException ex) {
                    logger.warn("This should'nt happen!");
                    logger.warn(ex.getMessage(),ex);
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
    public Model createJenaModelFromJSONLDString(String modelString) throws IllegalArgumentException {
        Graph graph = GraphFactory.createDefaultGraph();

        try (InputStream in = new ByteArrayInputStream(modelString.getBytes("UTF-8"))) {
            RDFParser.create()
                    .source(in)
                    .lang(Lang.JSONLD)
                    .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                    .parse(graph);
        } catch (UnsupportedEncodingException ex) {
            logger.error("Unsupported encoding", ex);
            throw new IllegalArgumentException("Could not parse the model");
        } catch(IOException ex) {
            logger.error("IO Exp JSON-LD", ex);
            throw new IllegalArgumentException("Could not parse the model");
        } catch(Exception ex) {
            logger.error("Unexpected exception", ex);
            throw new IllegalArgumentException("Could not parse the model");
        }

        if(graph.size()>0) {
            return ModelFactory.createModelForGraph(graph);
        } else {
            throw new IllegalArgumentException("Could not parse the model");
        }

    }
}