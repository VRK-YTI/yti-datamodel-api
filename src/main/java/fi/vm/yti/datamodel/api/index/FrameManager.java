package fi.vm.yti.datamodel.api.index;

import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import java.text.SimpleDateFormat;

@Singleton
@Service
public final class FrameManager {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);
    // private static final String ELASTIC_INDEX_VIS_MODEL = "dm_vis_models";
    private final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private final RestHighLevelClient esClient;
    private final ElasticConnector esManager;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;

    @Autowired
    public FrameManager(
        final ElasticConnector esManager,
        final JenaClient jenaClient,
        final ModelManager modelManager) {
        this.esManager = esManager;
        this.esClient = esManager.getEsClient();
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
    }

    /*
    public void cleanCachedFrames(boolean createInAnyCase) throws IOException {
        if (esManager.cleanIndex(ELASTIC_INDEX_VIS_MODEL) || createInAnyCase) {
            esManager.createIndex(ELASTIC_INDEX_VIS_MODEL);
        } else {
            logger.info("Index \"" + ELASTIC_INDEX_VIS_MODEL + "\" not found, thus not cleaned nor re-created.");
        }
    }
     */

    /*
    public String getCachedClassVisualizationFrame(String id,
                                                   Date lastModified) throws Exception {
        logger.info("Getting framed json-ld from cache: " + id);
        String encId = LDHelper.encode(id);
        String frameStr = null;
        try {
            Map<String, Object> map = esClient.get(new GetRequest(ELASTIC_INDEX_VIS_MODEL, "doc", encId), RequestOptions.DEFAULT).getSourceAsMap();
            if (map == null) {
                logger.debug("Creating visualization frame cache for graph " + id);
                frameStr = updateCachedGraph(id);
            } else {
                Object lastModifiedDate = map.get("modified");
                if (lastModified != null && lastModifiedDate != null && lastModified.after(format.parse(lastModifiedDate.toString()))) {
                    logger.debug("Updating visualization frame: " + id);
                    frameStr = updateCachedGraph(id);
                } else {
                    logger.debug("Visualization frame cache hit: " + id);
                    frameStr = map.get("graph").toString();
                }
            }
        } catch (ElasticsearchException ex) {
            logger.error("Datamodel Elastic is not available. Model frame caching is not available.");
            frameStr = graphToFramedString(id, Frames.classVisualizationFrame);
        } catch (Exception e) {
            logger.error("Unhandled exception while getting visualization frame", e);
            throw e;
        }
        return frameStr;
    }*/

    /*
    public void cacheClassVisualizationFrame(String id,
                                             String framed) {
        String encId = LDHelper.encode(id);

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            try (XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, framed)) {
                builder.startObject();
                builder.field("modified", format.format(new Date()));
                builder.field("graph", framed);
                builder.endObject();
                IndexRequest updateReq = new IndexRequest(ELASTIC_INDEX_VIS_MODEL, "doc", encId);
                updateReq.source(builder);
                IndexResponse resp = esClient.index(updateReq, RequestOptions.DEFAULT);
                logger.info("Index update response: " + resp.status().getStatus());
                builder.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            logger.error("Could not cache visualization frame for id " + id, ex);
        }
    }*/

    /**
     * Creates export graph by joining all the resources to one graph
     *
     * @param graph model IRI that is used to create export graph
     */
    public Model constructExportGraph(String graph) {

        String queryString = "CONSTRUCT { "
            + "?model <http://purl.org/dc/terms/hasPart> ?resource . "
            + "?rs ?rp ?ro . "
            + " } WHERE {"
            + " GRAPH ?model {"
            + "?model a owl:Ontology . "
            + "} OPTIONAL {"
            + "GRAPH ?modelHasPartGraph { "
            + " ?model <http://purl.org/dc/terms/hasPart> ?resource . "
            + " } GRAPH ?resource { "
            + "?rs ?rp ?ro . "
            + "}"
            + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", graph);
        pss.setIri("modelHasPartGraph", graph + "#HasPartGraph");

        Query query = pss.asQuery();
        Model exportModel = jenaClient.getModelFromCore(graph);

        if (exportModel != null) {
            Model exportModelConstruct = jenaClient.constructFromService(query.toString(), jenaClient.getEndpointServices().getCoreSparqlAddress());
            exportModel.add(exportModelConstruct);
            return exportModel;
        }
        throw new NotFoundException("Could not found graph " + graph);
    }

    /*
    protected String graphToFramedString(String graph,
                                         LinkedHashMap<String, Object> frame) throws Exception {
        Model model = jenaClient.getModelFromCore(graph + "#ExportGraph");
        if (model == null) {
            // In some rare cases ExportGraph is not found at this point.
            // Reconstruct graph in those cases instead of throwing exception
            logger.warn("Could not find ExportGraph: " + graph);
            model = constructExportGraph(graph);
        }

        // Copy frame content because it will be modified later
        LinkedHashMap<String, Object> frameCopy = new LinkedHashMap<>();

        frame.entrySet().forEach(entry -> {
            if (entry.getValue() instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> listCopy = new LinkedHashMap<>();

                ((LinkedHashMap<String, Object>) entry.getValue()).entrySet().forEach(subEntry -> {
                    listCopy.put(subEntry.getKey(), subEntry.getValue());
                });
                frameCopy.put(entry.getKey(), listCopy);
            } else {
                frameCopy.put(entry.getKey(), entry.getValue());
            }
        });
        return graphToFramedString(model, frameCopy);
    }

    protected String graphToFramedString(Model model,
                                         LinkedHashMap<String, Object> frame) throws Exception {

        String framed;
        try (StringWriter stringWriter = new StringWriter()) {
            PrefixMap pm = RiotLib.prefixMap(model.getGraph());

            pm = modelManager.cleanUpPrefixes(pm);
            pm.putAll(LDHelper.PREFIX_MAP);

            ((LinkedHashMap<String, Object>) frame.get("@context")).putAll(pm.getMappingCopyStr());
            JsonLdOptions opts = new JsonLdOptions();
            opts.setProcessingMode(opts.JSON_LD_1_1);
            opts.useNamespaces = true;
            opts.setCompactArrays(true);
            opts.setUseNativeTypes(Boolean.TRUE);
            JsonLDWriteContext ctx = new JsonLDWriteContext();
            ctx.setFrame(frame);
            ctx.setOptions(opts);
            WriterGraphRIOT gw = RDFDataMgr.createGraphWriter(RDFFormat.JSONLD_FRAME_PRETTY);
            gw.write(stringWriter, model.getGraph(), pm, null, ctx);
            framed = stringWriter.toString();
        }
        return framed;
    }
     */

    /*
    private String updateCachedGraph(String id) throws Exception {
        String frameStr = graphToFramedString(id, Frames.classVisualizationFrame);
        cacheClassVisualizationFrame(id, frameStr);
        return frameStr;
    }*/
}
