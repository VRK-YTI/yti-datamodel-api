package fi.vm.yti.datamodel.api.utils;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * Created by malonen on 10.2.2018.
 */
public class ElasticJsonLD {

    public static IndexRequest createIndexRequestFromModel(String index, String id, Model model) {
         IndexRequest request = new IndexRequest(index, "_doc", id);
         request.source(FrameManager.toJsonLDStringFromModel(model),XContentType.JSON);
         return request;
    }

    public static IndexRequest createIndexRequestFromFramedModel(String index, String id, Model model, String frame) {
        IndexRequest request = new IndexRequest(index, "_doc", id);
        request.source(FrameManager.modelToFramedString(model, frame),XContentType.JSON);
        return request;
    }

    public static BulkRequest createBulkIndexRequestFromModel(String index, Model model, Resource type) {
        BulkRequest request = new BulkRequest();
        ResIterator res = model.listSubjectsWithProperty(RDF.type, type);
        int i = 0;
        while(res.hasNext()) {
            i = i+1;
            Resource resource = res.nextResource();
            if(resource.isURIResource()) {
                StmtIterator statements = resource.listProperties();
                Model resourceModel = statements.toModel();

                System.out.println(""+i);

                request.add(new IndexRequest(index,"_doc",""+i)
                        .source(FrameManager.toJsonLDStringFromModel(resourceModel),XContentType.JSON));
            }
        }

        return request;
    }




}
