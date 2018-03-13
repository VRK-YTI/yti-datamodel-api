package fi.vm.yti.datamodel.api.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticJsonLD {

    private final FrameManager frameManager;

    @Autowired
    ElasticJsonLD(FrameManager frameManager) {
        this.frameManager = frameManager;
    }

    public IndexRequest createIndexRequestFromModel(String index, String id, Model model) {
         IndexRequest request = new IndexRequest(index, "_doc", id);
         request.source(frameManager.toJsonLDStringFromModel(model),XContentType.JSON);
         return request;
    }

    public IndexRequest createIndexRequestFromFramedModel(String index, String id, Model model, String frame) {
        IndexRequest request = new IndexRequest(index, "_doc", id);
        request.source(frameManager.modelToFramedString(model, frame),XContentType.JSON);
        return request;
    }

    public BulkRequest createBulkIndexRequestFromModel(String index, Model model, Resource type) {
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
                        .source(frameManager.toJsonLDStringFromModel(resourceModel),XContentType.JSON));
            }
        }

        return request;
    }
}
