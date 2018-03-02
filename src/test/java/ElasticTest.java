import fi.vm.yti.datamodel.api.utils.ElasticJsonLD;
import org.apache.http.HttpHost;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.SKOS;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Ignore;
import org.junit.Test;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by malonen on 10.2.2018.
 */
public class ElasticTest {

    private static final Logger logger = Logger.getLogger(ElasticTest.class.getName());


    //TODO: Test framed json-ld objects in elastic

    @Ignore
    @Test
    public void testElastic() {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

      /*  BulkRequest request = new BulkRequest();
        request.add(new IndexRequest("test3","_doc","1")
                .source(XContentType.JSON,"field","foo"));
        request.add(new IndexRequest("test3","_doc","2")
                .source(XContentType.JSON, "field","bar"));
        */

      BulkRequest request = ElasticJsonLD.createBulkIndexRequestFromModel("ptvltest",FileManager.get().loadModel("ptvl-skos.rdf"), SKOS.Concept);

        try{
            BulkResponse bulkResponse = client.bulk(request);
            logger.info("Status: "+bulkResponse.status());
            client.close();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }

    }
}
