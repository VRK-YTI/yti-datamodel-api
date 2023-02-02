package fi.vm.yti.datamodel.api.index;

import java.io.IOException;
import java.util.Map;

import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.opensearch.action.DocWriteRequest;

import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.transport.NodeDisconnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenSearchConnector {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchConnector.class);
    private static final int ES_TIMEOUT = 300;

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;


    @Autowired
    public OpenSearchConnector(final RestHighLevelClient client,
                               final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public boolean indexExists(String index) throws IOException {
        return client.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT);
    }

    public void waitForESNodes() {
        logger.info("Waiting for ES (timeout " + ES_TIMEOUT + "s)");
        try {
            for (int i = 0; i < ES_TIMEOUT; i++) {
                if (client.ping(RequestOptions.DEFAULT)) {
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
            this.client.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
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
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            logger.debug("Index \"" + index + "\" created: " + createIndexResponse.isAcknowledged());
        } catch (IOException ex) {
            logger.warn("Index creation failed for \"" + index + "\"", ex);
        }
    }

    // TODO is this needed
    public void updateMapping(String index,
                              String mapping) {
        PutMappingRequest request = new PutMappingRequest(index);
        try {
            request.source(mapping, XContentType.JSON);
            AcknowledgedResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
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
            IndexRequest indexReq = new IndexRequest(index)
                .id(encId)
                .source(objectMapper.convertValue(obj, Map.class), XContentType.JSON)
                .opType(DocWriteRequest.OpType.CREATE);
            IndexResponse resp = client.index(indexReq, RequestOptions.DEFAULT);
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
            updateReq.id(encId);
            updateReq.doc(objectMapper.convertValue(obj, Map.class), XContentType.JSON);
            UpdateResponse resp = client.update(updateReq, RequestOptions.DEFAULT);
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
            DeleteRequest req = new DeleteRequest(index)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            req.id(encId);
            DeleteResponse resp = client.delete(req, RequestOptions.DEFAULT);
            logger.info("Removed \"" + id + "\" from \"" + index + "\": " + resp.status().getStatus() + " (took " + (System.currentTimeMillis() - startTime) + " ms)");
            return resp;
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return null;
        }
    }
}
