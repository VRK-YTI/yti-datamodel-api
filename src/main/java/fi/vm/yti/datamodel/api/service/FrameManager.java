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
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.inject.Singleton;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;

@Singleton
@Service
public final class FrameManager {

    private final Client client;
    private static final Logger logger = Logger.getLogger(FrameManager.class.getName());
    
    
    public static final String ELASTIC_INDEX_MODEL = "dm_vis_models";
    public static final String ELASTIC_INDEX_RESOURCE = "dm_resources";

    @Autowired
    public FrameManager(final Client client) {
        this.client = client;
    }

    public String getCachedClassVisualizationFrame(String id) {
        String encId = LDHelper.encode(id);
        return this.client.prepareGet(ELASTIC_INDEX_MODEL, "doc", encId).execute().actionGet().getSourceAsString();
    }
    
    public void cacheClassVisualizationFrame(String id, Model model) {
        String encId = LDHelper.encode(id);
        String framed = modelToFramedString(model, Frames.classVisualizationFrame);
        IndexRequestBuilder rb = this.client.prepareIndex(ELASTIC_INDEX_MODEL, "doc", encId);
        rb.setSource(framed);
        rb.execute().actionGet();        
    }
    
    protected String modelToFramedString(Model model, LinkedHashMap<String, Object> frame) {

        PrefixMap pm = RiotLib.prefixMap(model.getGraph());
        StringWriter stringWriter = new StringWriter();
        
        ((LinkedHashMap<String, Object>)frame.get("@context")).putAll(pm.getMappingCopyStr());
        
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
     
    public void initCache() {
        boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_MODEL).execute().actionGet().isExists();
        if(!exists) {
            client.admin().indices().prepareCreate(ELASTIC_INDEX_MODEL).execute().actionGet();
        }
        exists = client.admin().indices().prepareExists(ELASTIC_INDEX_RESOURCE).execute().actionGet().isExists();
        if(!exists) {
            client.admin().indices().prepareCreate(ELASTIC_INDEX_RESOURCE).execute().actionGet();
        }
    }
    

}
