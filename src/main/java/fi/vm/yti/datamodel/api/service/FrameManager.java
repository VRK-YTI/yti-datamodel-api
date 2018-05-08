package fi.vm.yti.datamodel.api.service;

import com.github.jsonldjava.core.JsonLdOptions;
import fi.vm.yti.datamodel.api.utils.Frames;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

@Singleton
@Service
public final class FrameManager {

    private final Client esClient;
    private final JenaClient jenaClient;

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class.getName());
    private final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    
    public static final String ELASTIC_INDEX_MODEL = "dm_vis_models";
    public static final String ELASTIC_INDEX_RESOURCE = "dm_resources";
    public static final int ES_TIMEOUT = 120;

    @Autowired
    public FrameManager(
            final Client esClient,
            final JenaClient jenaClient) {
        this.esClient = esClient;
        this.jenaClient = jenaClient;
    }


    public String getCachedClassVisualizationFrame(String id, Date lastModified) throws Exception {
        String encId = LDHelper.encode(id);
        String frameStr = null;
        try {
            Map<String, Object> map = this.esClient.prepareGet(ELASTIC_INDEX_MODEL, "doc", encId).execute().actionGet().getSourceAsMap();
            if(map == null || lastModified.after(format.parse(map.get("modified").toString()))) {
                logger.debug("Creating/refreshing visualization frame cache for graph " + id);
                frameStr = graphToFramedString(id, Frames.classVisualizationFrame);
                cacheClassVisualizationFrame(id, frameStr); 
            }
            else {
                logger.debug("Visualization frame cache hit");
                XContentBuilder b = XContentFactory.jsonBuilder();
                b.value(map.get("graph"));
                frameStr = b.string();
            }

        } catch (NoNodeAvailableException ex) {
            logger.error("Datamodel Elastic is not available. Model frame caching is not available."); 
            frameStr = graphToFramedString(id, Frames.classVisualizationFrame);            
        }
        return frameStr;
    }

    public void cacheClassVisualizationFrame(String id, String framed) {
        String encId = LDHelper.encode(id);
        IndexRequestBuilder rb = this.esClient.prepareIndex(ELASTIC_INDEX_MODEL, "doc", encId);        
        try(XContentBuilder builder = XContentFactory.jsonBuilder()) {
            try(XContentParser parser  = XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, framed)) {
                builder.startObject();
                builder.field("modified", format.format(new Date()));
                builder.field("graph", parser.map());
                builder.endObject();        
                builder.close();        
                rb.setSource(builder);
                rb.execute().actionGet();                
            }            
        }catch(Exception ex) {
            logger.error("Could not cache visualization frame for id " + id, ex);
        }
    }

    protected String graphToFramedString(String graph, LinkedHashMap<String, Object> frame) throws Exception {
        Model model = jenaClient.getModelFromCore(graph + "#ExportGraph");
        if(model == null) {
            throw new NotFoundException("Could not get model with id " + graph);
        }
        
        PrefixMap pm = RiotLib.prefixMap(model.getGraph());
        StringWriter stringWriter = new StringWriter();

        ((LinkedHashMap<String, Object>) frame.get("@context")).putAll(pm.getMappingCopyStr());

        JsonLdOptions opts = new JsonLdOptions();
        opts.useNamespaces = true;
        opts.setCompactArrays(true);

        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setFrame(frame);
        ctx.setOptions(opts);

        WriterGraphRIOT gw = RDFDataMgr.createGraphWriter(RDFFormat.JSONLD_FRAME_PRETTY);
        gw.write(stringWriter, model.getGraph(), pm, null, ctx);

        return stringWriter.toString();
    }

    private void waitForESNodes() {
        logger.info("Waiting for ES (timeout " + ES_TIMEOUT + "s)");
        try {
            for(int i = 0; i < ES_TIMEOUT; i++) {
                if(((TransportClient)esClient).connectedNodes().size() > 0) {                    
                    logger.info("ES online");
                    return;
                }                
                Thread.sleep(1000);                
            }
        }catch(Exception ex) {}
        throw new RuntimeException("Could not find required ES instance");
    }
    
    public void initCache() {
        waitForESNodes();
        boolean exists = esClient.admin().indices().prepareExists(ELASTIC_INDEX_MODEL).execute().actionGet().isExists();
        if (!exists) {
            esClient.admin().indices().prepareCreate(ELASTIC_INDEX_MODEL).execute().actionGet();
        }
        exists = esClient.admin().indices().prepareExists(ELASTIC_INDEX_RESOURCE).execute().actionGet().isExists();
        if (!exists) {
            esClient.admin().indices().prepareCreate(ELASTIC_INDEX_RESOURCE).execute().actionGet();
        }
    }

}
