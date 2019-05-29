package fi.vm.yti.datamodel.api.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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

    public void addToIndex(String index,
                           String id,
                           Object obj) {
        String encId = LDHelper.encode(id);
        try {
            IndexRequest updateReq = new IndexRequest(index, "doc", encId);
            updateReq.source(objectMapper.convertValue(obj, Map.class));
            IndexResponse resp = esClient.index(updateReq, RequestOptions.DEFAULT);
            logger.info("Index update response: " + resp.status().getStatus());
        } catch (IOException e) {
            logger.warn("Could not add to index: "+id);
            logger.warn(ExceptionUtils.getExceptionMessage(e));
        }
    }

    public DeleteResponse removeFromIndex(String id,
                                          String index) {
        try {
            return esClient.delete(new DeleteRequest(index, "doc", LDHelper.encode(id)), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.warn(ExceptionUtils.getExceptionMessage(e));
            return null;
        }
    }
}
