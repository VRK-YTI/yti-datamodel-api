package fi.vm.yti.datamodel.api.index;

import java.io.IOException;
import java.util.Map;

import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.NodeDisconnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ElasticConnector {

    private static final Logger logger = LoggerFactory.getLogger(ElasticConnector.class);
    private static final int ES_TIMEOUT = 300;

    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper;


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
                        indexExists("dm_does_not_exist");
                        return;
                    } catch (NodeDisconnectedException ex) {
                        logger.info("Node Disconnected?");
                    }
                }
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Delete an index if it exists.
     * @param index index name
     * @return true if the index existed and was removed
     * @throws IOException in case there is a problem sending the request or parsing back the response
     */
    public boolean cleanIndex(String index) throws IOException {
        boolean exists = indexExists(index);
        if (exists) {
            logger.info("Cleaning index: {}", index);
            this.esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
        }
        return exists;
    }

    public void createIndex(String index) {
        createIndex(index, null);
    }

    public void createIndex(String index,
                            String mapping) {
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            if (mapping != null && !mapping.isEmpty()) {
                request.source(mapping, XContentType.JSON);
            }
            CreateIndexResponse createIndexResponse = esClient.indices().create(request, RequestOptions.DEFAULT);
            logger.debug("Index \"" + index + "\" created: " + createIndexResponse.isAcknowledged());
        } catch (IOException ex) {
            logger.warn("Index creation failed for \"" + index + "\"", ex);
        }
    }

    public void updateMapping(String index,
                              Object mapping) {
        PutMappingRequest request = new PutMappingRequest(index);
        request.type("doc");
        try {
            request.source(mapping);
            AcknowledgedResponse putMappingResponse = esClient.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.debug("Mapping updated for \"" + index + "\": " + putMappingResponse.isAcknowledged());
        } catch (IOException ex) {
            logger.warn("Mapping update failed for \"" + index + "\"", ex);
        }
    }

    public void putToIndex(String index,
                           String id,
                           Object obj) {
        String encId = DataModelUtils.encode(id);
        try {
            IndexRequest indexReq = new IndexRequest(index, "doc", encId);
            indexReq.source(objectMapper.convertValue(obj, Map.class), XContentType.JSON);
            indexReq.opType(DocWriteRequest.OpType.CREATE);
            IndexResponse resp = esClient.index(indexReq, RequestOptions.DEFAULT);
            logger.info("Indexed \"" + id + "\" to \"" + index + "\": " + resp.status().getStatus());
        } catch (IOException e) {
            logger.warn("Could not add to index: " + id);
            logger.warn(e.getMessage());
        }
    }

    public void updateToIndex(String index,
                              String id,
                              Object obj) {
        String encId = DataModelUtils.encode(id);
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
            logger.warn(e.getMessage());
        }
    }

    public DeleteResponse removeFromIndex(String id,
                                          String index) {
        String encId = DataModelUtils.encode(id);
        try {
            // TODO: Consider IMMEDIATE refresh policy?
            final long startTime = System.currentTimeMillis();
            DeleteRequest req = new DeleteRequest(index, "doc", encId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            DeleteResponse resp = esClient.delete(req, RequestOptions.DEFAULT);
            logger.info("Removed \"" + id + "\" from \"" + index + "\": " + resp.status().getStatus() + " (took " + (System.currentTimeMillis() - startTime) + " ms)");
            return resp;
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return null;
        }
    }
}
