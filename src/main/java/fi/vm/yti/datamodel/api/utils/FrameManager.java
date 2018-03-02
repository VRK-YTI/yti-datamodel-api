package fi.vm.yti.datamodel.api.utils;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;

import javax.json.Json;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by malonen on 10.2.2018.
 */
public class FrameManager {


    public static JsonLDWriteContext getDefaultContext(RDFFormat format, Model model, String id, Object frame) {
        DatasetGraph g = DatasetFactory.create(model).asDatasetGraph();
        return getDefaultContext(format, (JsonLDWriteContext) g.getContext(), id, frame);
    }

    public static JsonLDWriteContext getDefaultContext(RDFFormat format, JsonLDWriteContext ctx, String id, Object frame) {

        if(ctx==null) ctx = new JsonLDWriteContext();

        if(format.equals(RDFFormat.JSONLD_FRAME_FLAT) || format.equals(RDFFormat.JSONLD_FRAME_PRETTY)) {
            if(frame==null) {
                Map<String, Object> idFrame = new HashMap<String, Object>();
                idFrame.put("@id", id);
                ctx.setFrame(idFrame);
            } else {
                ctx.setFrame(frame);
            }
        }

        JsonLdOptions opts = new JsonLdOptions();
        opts.useNamespaces = true;
        opts.setCompactArrays(true);
        ctx.setOptions(opts);

        // opts.setUseNativeTypes(true);
        // opts.setPruneBlankNodeIdentifiers(true);
        // opts.setEmbed(true);
        // opts.setExplicit(true);
        // opts.setOmitDefault(true);
        // opts.setProduceGeneralizedRdf(true);

        return ctx;
    }

    public String frameClass(Model model) {

        Object frame = new LinkedHashMap<String, Object>() {
            {
                put("@type", new ArrayList<Object>() {
                    {
                        add("rdfs:Class");
                        add("sh:Shape");
                    }
                });
            }
        };

        //Object classModel = toJsonLDObject(model, RDFFormat.JSONLD);
        //Object classContext = ((LinkedHashMap<String, Object>) classModel).get("@context");
        JsonLDWriteContext ctx = getDefaultContext(RDFFormat.JSONLD_FRAME_PRETTY, model, null, frame);


        return toJsonLDStringFromModel(model, RDFFormat.JSONLD_FRAME_PRETTY, ctx);
    }


    public static String toJsonLDStringFromModel(Model model) {
        return toJsonLDStringFromModel(model, RDFFormat.JSONLD, null);
    }

    public static String toJsonLDStringFromModel(Model model, RDFFormat format, JsonLDWriteContext ctx) {
        if(!format.getLang().equals(Lang.JSONLD)) {
            throw new IllegalArgumentException();
        }
        try {
            return JsonUtils.toPrettyString(toJsonLDObject(model, format, ctx));
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object toJsonLDObject(Model m, RDFFormat format) {

        return toJsonLDObject(m, format, null);

    }


    public static String modelToFramedString(Model model, String frame) {

        PrefixMap pm = RiotLib.prefixMap(model.getGraph());
        StringWriter stringWriter = new StringWriter();

        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setFrame(frame);

        WriterGraphRIOT gw = RDFDataMgr.createGraphWriter(RDFFormat.JSONLD_FRAME_PRETTY);
        gw.write(stringWriter, model.getGraph(), pm, null, ctx);

        return stringWriter.toString();
    }

    public static Object toJsonLDObject(Model m, RDFFormat format, JsonLDWriteContext context) {
        DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        PrefixMap pm = RiotLib.prefixMap(g);
        if(context==null) {
            context = getDefaultContext(format, (JsonLDWriteContext) g.getContext(), null, null);
        }
        try {
            return JsonLDWriter.toJsonLDJavaAPI((RDFFormat.JSONLDVariant) format.getVariant(), g, pm, null, context);
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        } catch(JsonLdError ex) {
            ex.printStackTrace();
            return null;
        }
    }


}
