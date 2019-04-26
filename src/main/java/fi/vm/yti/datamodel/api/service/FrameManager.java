package fi.vm.yti.datamodel.api.service;

import com.github.jsonldjava.core.JsonLdOptions;
import fi.vm.yti.datamodel.api.utils.Frames;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.elasticsearch.transport.NodeDisconnectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import org.apache.jena.iri.IRI;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

@Singleton
@Service
public final class FrameManager {

    private final RestHighLevelClient esClient;
    private final JenaClient jenaClient;
    
    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class.getName());
    private final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    
    public static final String ELASTIC_INDEX_MODEL = "dm_vis_models";
    public static final String ELASTIC_INDEX_RESOURCE = "dm_resources";
    public static final int ES_TIMEOUT = 300;

    @Autowired
    public FrameManager(
            final RestHighLevelClient esClient,
            final JenaClient jenaClient) {
        this.esClient = esClient;
        this.jenaClient = jenaClient;
    }

    public void cleanCachedFrames() throws IOException {
        boolean exists = indexExists(ELASTIC_INDEX_MODEL);
        if (exists) {
            logger.info("Cleaning elastic index");
            this.esClient.indices().delete(new DeleteIndexRequest(ELASTIC_INDEX_MODEL), RequestOptions.DEFAULT);
            //esClient.admin().indices().prepareDelete(ELASTIC_INDEX_MODEL).execute().actionGet();
            initCache();
        } else {
            logger.info("No index found for cleaning!");
        }
    }

    private String updateCachedGraph(String id) throws Exception {
        String frameStr = graphToFramedString(id, Frames.classVisualizationFrame);
        cacheClassVisualizationFrame(id, frameStr);
        return frameStr;
    }

    public String getCachedClassVisualizationFrame(String id, Date lastModified) throws Exception {
        logger.info("Getting framed json-ld from cache: "+id);
        String encId = LDHelper.encode(id);
        String frameStr = null;
        try {
        	Map<String, Object> map = esClient.get(new GetRequest(ELASTIC_INDEX_MODEL,"doc",encId), RequestOptions.DEFAULT).getSourceAsMap();
            if(map == null) {
                logger.debug("Creating visualization frame cache for graph " + id);
                frameStr = updateCachedGraph(id);
            }
            else {
                Object lastModifiedDate = map.get("modified");
                if(lastModified!=null && lastModifiedDate!=null && lastModified.after(format.parse(lastModifiedDate.toString()))) {
                    logger.debug("Updating visualization frame: "+id);
                    frameStr = updateCachedGraph(id);
                } else {
                    logger.debug("Visualization frame cache hit: "+id);
                    frameStr = map.get("graph").toString();
                }
            }
        } catch (ElasticsearchException ex) {
            logger.error("Datamodel Elastic is not available. Model frame caching is not available."); 
            frameStr = graphToFramedString(id, Frames.classVisualizationFrame);            
        }
        return frameStr;
    }
    
    public void cacheClassVisualizationFrame(String id, String framed) {
        String encId = LDHelper.encode(id);
            
        try(XContentBuilder builder = XContentFactory.jsonBuilder()) {
            try(XContentParser parser  = XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY,DeprecationHandler.THROW_UNSUPPORTED_OPERATION,framed)) {
                builder.startObject();
                builder.field("modified", format.format(new Date()));
                builder.field("graph", framed);
                builder.endObject();        
                IndexRequest updateReq = new IndexRequest(ELASTIC_INDEX_MODEL,"doc",encId);
                updateReq.source(builder);
                IndexResponse resp = esClient.index(updateReq, RequestOptions.DEFAULT);
                logger.info("Index update response: "+resp.status().getStatus());
                builder.close();             
            }            
        }catch(Exception ex) {
        	ex.printStackTrace(System.out);
            logger.error("Could not cache visualization frame for id " + id, ex);
        }
    }

    /**
     * Creates export graph by joining all the resources to one graph
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
        pss.setIri("modelHasPartGraph", graph+"#HasPartGraph");

        Query query = pss.asQuery();
        Model exportModel = jenaClient.getModelFromCore(graph);
        Model exportModelConstruct = jenaClient.constructFromService(query.toString(), jenaClient.getEndpointServices().getCoreSparqlAddress());
        exportModel.add(exportModelConstruct);
        return exportModel;
    }

    protected String graphToFramedString(String graph, LinkedHashMap<String, Object> frame) throws Exception {
        Model model = jenaClient.getModelFromCore(graph + "#ExportGraph");
        // Model model = constructExportGraph(graph);
        if(model == null) {
            throw new NotFoundException("Could not get model with id " + graph);
        }
        return graphToFramedString(model, frame);
    }
    
    protected String graphToFramedString(Model model, LinkedHashMap<String, Object> frame) throws Exception {
        
        String framed;
        try (StringWriter stringWriter = new StringWriter()) {
            PrefixMap pm = RiotLib.prefixMap(model.getGraph());
            
            pm = cleanUpPrefixes(pm);
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

    private PrefixMap cleanUpPrefixes(PrefixMap map) {
        final Pattern pattern = Pattern.compile("j\\.\\d+");
        List<String> toBeDeleted = new ArrayList<String>();
        map.forEach((String t, IRI u) -> {
            Matcher matcher = pattern.matcher(t);
            if(matcher.lookingAt()) {
                toBeDeleted.add(t);
            }
        });
        toBeDeleted.forEach((String t) -> {
            map.delete(t);
        });
        return map;
    }
    
    private void waitForESNodes() {
        logger.info("Waiting for ES (timeout " + ES_TIMEOUT + "s)");
        try {
            for(int i = 0; i < ES_TIMEOUT; i++) {
                if(esClient.ping(RequestOptions.DEFAULT)) {
                    logger.info("ES online");
                    try{
                        indexExists(ELASTIC_INDEX_MODEL);
                        return;
                    } catch (NodeDisconnectedException ex) {
                        logger.info("Node Disconnected?");
                    }
                }
                Thread.sleep(1000);
            }
        } catch(Exception ex) {}
        throw new RuntimeException("Could not find required ES instance");
    }

    private boolean indexExists(String index) throws IOException {
    	return esClient.indices().exists(new GetIndexRequest().indices(index), RequestOptions.DEFAULT);
    }

    public void initCache() throws IOException {
        waitForESNodes();
        boolean exists = indexExists(ELASTIC_INDEX_MODEL);
        if (!exists) {
        	esClient.indices().create(new CreateIndexRequest(ELASTIC_INDEX_MODEL), RequestOptions.DEFAULT);
        }
        exists = indexExists(ELASTIC_INDEX_RESOURCE);
        if (!exists) {
        	esClient.indices().create(new CreateIndexRequest(ELASTIC_INDEX_RESOURCE), RequestOptions.DEFAULT);
        }
    }
    
    public DeleteResponse removeCachedModel(String id) throws IOException {
    	return esClient.delete(new DeleteRequest(ELASTIC_INDEX_MODEL,"doc",LDHelper.encode(id)), RequestOptions.DEFAULT);
    }
    
    public DeleteResponse removeCachedResource(String id) throws IOException {
    	return esClient.delete(new DeleteRequest(ELASTIC_INDEX_RESOURCE,"doc",LDHelper.encode(id)), RequestOptions.DEFAULT);
    }

}
