/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.graph.Graph;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ModelManager {

    private static final Logger logger = LoggerFactory.getLogger(ModelManager.class.getName());

    /**
     * Writes jena model to string
     *
     * @param model model to be written as json-ld string
     * @return string
     */
    public String writeModelToJSONLDString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD);
        return writer.toString();
    }

    public String writeModelToString(Model model,
                                     RDFFormat format) {
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
        } catch (IOException ex) {
            logger.warn(ex.getMessage(), ex);
            return null;
        }
    }

    public String writeModelToJSONLDString(Model m,
                                           Context jenaContext) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
            DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
            PrefixMap pm = RiotLib.prefixMap(g);
            //PrefixMap pm = PrefixMapFactory.create(m.getNsPrefixMap());
            w.write(out, g, pm, null, jenaContext);
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object stringToFramedObject(String graphString,
                                       LinkedHashMap<String, Object> newFrame) throws IOException {
        Object jsonObject = JsonUtils.fromString(graphString);
        JsonLdOptions opts = new JsonLdOptions();
        opts.setProcessingMode(opts.JSON_LD_1_1);
        opts.useNamespaces = true;
        opts.setCompactArrays(true);
        opts.setUseNativeTypes(Boolean.TRUE);
        Object framed = JsonLdProcessor.frame(jsonObject, newFrame, opts);
        framed = ((ArrayList) ((Map) framed).get("@graph")).get(0);
        return framed;
    }

    public Object reFramedStringToObject(String graphString,
                                         LinkedHashMap<String, Object> originalFrame,
                                         LinkedHashMap<String, Object> newFrame) throws IOException {
        Object jsonObject = JsonUtils.fromString(graphString);
        ((Map) jsonObject).put("@context", originalFrame.get("@context"));
        JsonLdOptions opts = new JsonLdOptions();
        opts.setProcessingMode(opts.JSON_LD_1_1);
        opts.useNamespaces = true;
        opts.setCompactArrays(true);
        opts.setUseNativeTypes(Boolean.TRUE);
        Object framed = JsonLdProcessor.frame(jsonObject, newFrame, opts);
        framed = ((ArrayList) ((Map) framed).get("@graph")).get(0);
        return framed;
    }

    public String mapObjectToString(Object jsonNode) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }

    public JsonNode toFramedJsonNode(Model model,
                                     LinkedHashMap<String, Object> frame) throws IOException {
        WriterGraphRIOT gw = RDFDataMgr.createGraphWriter(RDFFormat.JSONLD_FRAME_PRETTY);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Graph modelGraph = model.getGraph();
        PrefixMap pm = RiotLib.prefixMap(modelGraph);
        pm = cleanUpPrefixes(pm);
        pm.putAll(LDHelper.PREFIX_MAP);
        ((LinkedHashMap<String, Object>) frame.get("@context")).putAll(pm.getMappingCopyStr());
        JsonLdOptions opts = new JsonLdOptions();
        opts.setProcessingMode(opts.JSON_LD_1_1);
        opts.useNamespaces = false;
        opts.setCompactArrays(true);
        opts.setUseNativeTypes(Boolean.TRUE);
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setFrame(frame);
        ctx.setOptions(opts);
        gw.write(baos, model.getGraph(), pm, null, ctx);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(baos.toByteArray());
        return jsonNode;
    }

    public String toPlainJsonString(JsonNode jsonNode) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode.get("@graph"));
    }

    public String toPlainJsonString(Model model,
                                    LinkedHashMap<String, Object> frame) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = toFramedJsonNode(model, frame);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode.get("@graph"));
    }

    public PrefixMap cleanUpPrefixes(PrefixMap map) {
        final Pattern pattern = Pattern.compile("j\\.\\d+");
        List<String> toBeDeleted = new ArrayList<String>();
        map.forEach((String t, IRI u) -> {
            Matcher matcher = pattern.matcher(t);
            if (matcher.lookingAt()) {
                toBeDeleted.add(t);
            }
        });
        toBeDeleted.forEach((String t) -> {
            map.delete(t);
        });
        return map;
    }

    public void removeLanguages(Model model,
                                List<String> langList) {
        StmtIterator listIterator = model.listStatements();
        List<Statement> removeStatements = new ArrayList<>();
        if (langList == null || langList.isEmpty()) return;
        while (listIterator.hasNext()) {
            Statement langStatement = listIterator.next();
            if (langStatement.getObject().isLiteral()) {
                Literal lit = langStatement.getLiteral();
                String litLang = lit.getLanguage();
                if (litLang != null && !litLang.isEmpty()) {
                    if (!langList.contains(litLang)) {
                        removeStatements.add(langStatement);
                    }
                }
            }
        }
        model.remove(removeStatements);
    }

    public Model removeListStatements(Model resource,
                                      Model model) {

        StmtIterator listIterator = resource.listStatements();

        // OMG: Iterate trough all RDFLists and remove old lists from exportModel
        while (listIterator.hasNext()) {
            Statement listStatement = listIterator.next();
            if (!listStatement.getSubject().isAnon() && listStatement.getObject().isAnon()) {
                Statement removeStatement = model.getRequiredProperty(listStatement.getSubject(), listStatement.getPredicate());
                RDFList languageList = removeStatement.getObject().as(RDFList.class);
                languageList.removeList();
                removeStatement.remove();

            }
        }

        return model;

    }

    // TODO: Not working / or in use yet.

    /**
     * Changes property UUIDs in model
     *
     * @param model Shape model to be used in UUID rename
     *              returns Model with changed UUIDs
     */
    public Model updatePropertyUUIDs(Model model) {

        String query =
            "DELETE { GRAPH ?shape { ?shape sh:property ?prop . ?prop ?p ?o .  } } "
                + "INSERT { GRAPH ?shape { ?shape sh:property ?newProp . ?newProp ?p ?o . } } "
                + "WHERE { GRAPH ?shape { ?shape sh:property ?prop . BIND(UUID() AS ?newProp) ?prop ?p ?o . }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(query);
        UpdateRequest queryObj = pss.asUpdate();
        UpdateAction.execute(queryObj, model);

        return model;

    }

    /**
     * Removes all triples (one level of anonymous nodes) from MODEL that are in RESOURCE except those resources that are type owl:Ontology
     *
     * @param resource Resource to be removed from the model
     * @param model    Model where resource is removed from
     * @return Returns model where other model is removed
     */
    public Model removeResourceStatements(Model resource,
                                          Model model) {

        StmtIterator listIterator = resource.listStatements();

        List<Statement> statementsToRemove = new ArrayList<Statement>();
        List<RDFList> listsToRemove = new ArrayList<RDFList>();

        while (listIterator.hasNext()) {
            Statement listStatement = listIterator.next();
            Resource subject = listStatement.getSubject();
            RDFNode object = listStatement.getObject();
            Property listPredicate = listStatement.getPredicate();

            // If object is anonymous REMOVE object triples and all containing lists
            // MISSES second level anon nodes (Currently not an issue?)
            if (subject.isURIResource() && object.isAnon()) {
                try {
                    Statement removeStatement = model.getRequiredProperty(subject, listPredicate);
                    if (!removeStatement.getObject().isAnon()) {
                        logger.warn("This should'nt happen!");
                        logger.warn("Bad data " + subject.toString() + "->" + listPredicate);
                    } else if (removeStatement.getObject().canAs(RDFList.class)) {
                        // If object is list
                        RDFList languageList = removeStatement.getObject().as(RDFList.class);
                        languageList.removeList();
                        removeStatement.remove();
                    } else {
                        // If object is Anon such as sh:constraint
                        StmtIterator anonIterator = removeStatement.getObject().asResource().listProperties();
                        while (anonIterator.hasNext()) {
                            Statement anonStatement = anonIterator.next();
                            Resource anonSubject = anonStatement.getSubject();
                            RDFNode anonSubObject = anonStatement.getObject();
                            Property anonListPredicate = anonStatement.getPredicate();
                            if (anonSubObject.isAnon() && anonSubObject.canAs(RDFList.class)) {
                                // If Anon object has list such as sh:and
                                listsToRemove.add(anonSubObject.as(RDFList.class));
                            }
                            // remove statement later
                            statementsToRemove.add(anonStatement);

                        }

                        removeStatement.remove();

                    }
                } catch (PropertyNotFoundException ex) {
                    logger.warn("This should'nt happen!");
                    logger.warn(ex.getMessage(), ex);
                }
                // Remove ALL triples that are part of resource node such as Class or Property. Keep Ontology triples.
            } else if (subject.isURIResource() && !subject.hasProperty(RDF.type, OWL.Ontology)) {
                model.remove(listStatement);
            }
        }

        // Remove statements and lists after loop to avoid concurrent modification exception
        model.remove(statementsToRemove);

        for (Iterator<RDFList> i = listsToRemove.iterator(); i.hasNext(); ) {
            RDFList removeList = i.next();
            removeList.removeList();
        }

        return model;

    }

    /**
     * Create jena model from json-ld string
     *
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
        } catch (IOException ex) {
            logger.error("IO Exp JSON-LD", ex);
            throw new IllegalArgumentException("Could not parse the model");
        } catch (Exception ex) {
            logger.error("Unexpected exception", ex);
            throw new IllegalArgumentException("Could not parse the model");
        }

        if (graph.size() > 0) {
            return ModelFactory.createModelForGraph(graph);
        } else {
            throw new IllegalArgumentException("Could not parse the model");
        }

    }
}