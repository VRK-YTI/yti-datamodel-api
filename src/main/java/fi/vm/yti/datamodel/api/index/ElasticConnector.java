package fi.vm.yti.datamodel.api.index;

import java.io.IOException;
import java.util.Map;

import javax.inject.Singleton;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.NodeDisconnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.utils.LDHelper;

@Singleton
@Service
public class ElasticConnector {

    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper;

    public static final String ELASTIC_INDEX_VIS_MODEL = "dm_vis_models";
    public static final String ELASTIC_INDEX_RESOURCE = "dm_resources";
    public static final String ELASTIC_INDEX_MODEL = "dm_models";
    public static final String[] indexes = new String[]{ ELASTIC_INDEX_VIS_MODEL, ELASTIC_INDEX_MODEL, ELASTIC_INDEX_RESOURCE };

    public static final int ES_TIMEOUT = 300;

    private static final Logger logger = LoggerFactory.getLogger(ElasticConnector.class.getName());

    @Autowired
    public ElasticConnector(final RestHighLevelClient esClient,
                            final ObjectMapper objectMapper) {
        this.esClient = esClient;
        this.objectMapper = objectMapper;
    }

    public RestHighLevelClient getEsClient() {
        return esClient;
    }

    public boolean indexExists(String index) throws IOException {
        return esClient.indices().exists(new GetIndexRequest().indices(index), RequestOptions.DEFAULT);
    }

    public void waitForESNodes() {
        logger.info("Waiting for ES (timeout " + ES_TIMEOUT + "s)");
        try {
            for (int i = 0; i < ES_TIMEOUT; i++) {
                if (esClient.ping(RequestOptions.DEFAULT)) {
                    logger.info("ES online");
                    try {
                        indexExists(ELASTIC_INDEX_VIS_MODEL);
                        return;
                    } catch (NodeDisconnectedException ex) {
                        logger.info("Node Disconnected?");
                    }
                }
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
        }
        throw new RuntimeException("Could not find required ES instance");
    }

    public void cleanIndex(String index) throws IOException {
        boolean exists = indexExists(index);
        if (exists) {
            logger.info("Cleaning index: " + index);
            this.esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
        }
    }

    public void initCache() throws IOException {
        waitForESNodes();
        for (int i = 0; i < indexes.length; i++) {
            boolean exists = indexExists(indexes[i]);
            if (!exists) {
                esClient.indices().create(new CreateIndexRequest(indexes[i]), RequestOptions.DEFAULT);
            }
        }
    }

    public void putToIndex(String index,
                           String id,
                           Object obj) {
        String encId = LDHelper.encode(id);
        try {
            IndexRequest indexReq = new IndexRequest(index, "doc", encId);
            indexReq.source(objectMapper.convertValue(obj, Map.class), XContentType.JSON);
            indexReq.opType(DocWriteRequest.OpType.CREATE);
            IndexResponse resp = esClient.index(indexReq, RequestOptions.DEFAULT);
            logger.info("Indexed \"" + id + "\" to \"" + index + "\": " + resp.status().getStatus());
        } catch (IOException e) {
            logger.warn("Could not add to index: " + id);
            logger.warn(ExceptionUtils.getExceptionMessage(e));
        }
    }

    public void updateToIndex(String index,
                              String id,
                              Object obj) {
        String encId = LDHelper.encode(id);
        try {
            UpdateRequest updateReq = new UpdateRequest();
            updateReq.index(index);
            updateReq.type("doc");
            updateReq.id(encId);
            updateReq.doc(objectMapper.convertValue(obj, Map.class), XContentType.JSON);
            UpdateResponse resp = esClient.update(updateReq, RequestOptions.DEFAULT);
            logger.info("Updated \"" + id + "\" to \"" + index + "\": " + resp.status().getStatus());
        } catch (IOException e) {
            logger.warn("Could not update to index: " + id);
            logger.warn(ExceptionUtils.getExceptionMessage(e));
        }
    }

    public DeleteResponse removeFromIndex(String id,
                                          String index) {
        String encId = LDHelper.encode(id);
        try {
            DeleteResponse resp = esClient.delete(new DeleteRequest(index, "doc", encId), RequestOptions.DEFAULT);
            logger.info("Removed \"" + id + "\" from \"" + index + "\": " + resp.status().getStatus());
            return resp;
        } catch (IOException e) {
            logger.warn(ExceptionUtils.getExceptionMessage(e));
            return null;
        }
    }
}
