/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.*;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;

import fi.vm.yti.datamodel.api.utils.LDHelper;

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
        RDFDataMgr.write(writer, model, RDFFormat.JSONLD10_COMPACT_PRETTY);
        return writer.toString();
    }

    public String writeModelToString(Model model,
                                     RDFFormat format) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, format);
        return writer.toString();
    }

    public JsonNode toFramedJsonNode(Model model,
                                     LinkedHashMap<String, Object> frame) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Graph modelGraph = model.getGraph();
        PrefixMap pm = Prefixes.adapt(modelGraph);
        pm = cleanUpPrefixes(pm);
        pm.putAll(LDHelper.PREFIX_MAP);
        ((LinkedHashMap<String, Object>) frame.get("@context")).putAll(pm.getMappingCopy());
        JsonLdOptions opts = new JsonLdOptions();
        opts.setProcessingMode(JsonLdOptions.JSON_LD_1_1);
        opts.useNamespaces = true;
        opts.setCompactArrays(true);
        opts.setUseNativeTypes(Boolean.TRUE);
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setFrame(frame);
        ctx.setOptions(opts);

        RDFWriter w = RDFWriter.create().format(RDFFormat.JSONLD_FRAME_PRETTY)
                .source(model.getGraph())
                .context(ctx)
                .build();

        w.output(baos);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(baos.toByteArray());
    }

    public PrefixMap cleanUpPrefixes(PrefixMap map) {
        final Pattern pattern = Pattern.compile("j\\.\\d+");
        List<String> toBeDeleted = new ArrayList<>();
        map.forEach((String t, String u) -> {
            Matcher matcher = pattern.matcher(t);
            if (matcher.lookingAt()) {
                toBeDeleted.add(t);
            }
        });
        toBeDeleted.forEach(map::delete);
        return map;
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

        List<Statement> statementsToRemove = new ArrayList<>();
        List<RDFList> listsToRemove = new ArrayList<>();

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
                        logger.warn("This shouldn't happen!");
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
                            RDFNode anonSubObject = anonStatement.getObject();
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
                    logger.warn("This shouldn't happen!");
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
        PrefixMap pm = PrefixMapFactory.create();
        try {
            // FIXME: This is ugly hack for getting all of the prefixes from the json-ld context. For some reason urn namespaces are ignored by the parser.
            Map jsonObject = (Map) JsonUtils.fromString(modelString);
            Map context = (Map) jsonObject.get("@context");
            context.forEach((key, value) -> {
                if (value instanceof String && !LDHelper.isInvalidIRI((String) value)) {
                    pm.add((String) key, (String) value);
                }
            });
            try (InputStream in = new ByteArrayInputStream(modelString.getBytes(StandardCharsets.UTF_8))) {
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
                return ModelFactory.createModelForGraph(graph).setNsPrefixes(pm.getMappingCopy());
            } else {
                throw new IllegalArgumentException("Could not parse the model");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not parse the model");
        }
    }
}
