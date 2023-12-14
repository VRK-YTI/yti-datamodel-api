package fi.vm.yti.datamodel.api.index;

import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static fi.vm.yti.datamodel.api.v2.opensearch.OpenSearchUtil.logPayload;

@Service
public class OpenSearchConnector {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchConnector.class);
    private static final int ES_TIMEOUT = 300;
    private final OpenSearchClient client;

    @Autowired
    public OpenSearchConnector(final OpenSearchClient client) {
        this.client = client;
    }

    public OpenSearchClient getClient() {
        return client;
    }

    public boolean indexExists(String index) throws IOException {
        return client.indices().exists(
                new ExistsRequest.Builder().index(index).build()
        ).value();
    }

    public void waitForESNodes() {
        logger.info("Waiting for ES (timeout " + ES_TIMEOUT + "s)");
        try {
            for (var i = 0; i < ES_TIMEOUT; i++) {
                if (client.ping().value()) {
                    logger.info("ES online");
                    try {
                        indexExists("dm_does_not_exist");
                        return;
                    } catch (Exception ex) {
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
     *
     * @param index index name
     * @throws IOException in case there is a problem sending the request or parsing back the response
     */
    public void cleanIndex(String index) throws IOException {
        boolean exists = indexExists(index);
        if (exists) {
            logger.info("Cleaning index: {}", index);
            this.client.indices().delete(new DeleteIndexRequest.Builder()
                    .index(index).build());
        }
    }

    public void createIndex(String index, TypeMapping mappings) {
        var request = new CreateIndexRequest.Builder()
                .index(index)
                .mappings(mappings).build();
        logPayload(request, index);
        try {
            client.indices().create(request);
            logger.info("Index {} created", index);
        } catch (IOException | OpenSearchException ex) {
            logger.warn("Index creation failed for " + index, ex);
        }
    }

    public <T> void putToIndex(String index,
                               String id,
                               T doc) {
        String encId = DataModelUtils.encode(id);
        try {
            IndexRequest<T> indexReq = new IndexRequest.Builder<T>()
                    .index(index)
                    .refresh(Refresh.True)
                    .id(encId)
                    .document(doc)
                    .build();

            logPayload(indexReq, index);
            client.index(indexReq);
            logger.info("Indexed {} to {}}", id, index);
        } catch (IOException | OpenSearchException e) {
            logger.warn("Could not add to index: " + id, e);
        }
    }

    public <T> void updateToIndex(String index,
                                  String id,
                                  T doc) {
        String encId = DataModelUtils.encode(id);
        try {
            var request = new UpdateRequest.Builder<String, T>()
                    .index(index)
                    .refresh(Refresh.True)
                    .id(encId)
                    .doc(doc)
                    .build();
            logPayload(request, index);
            client.update(request, String.class);
            logger.info("Updated {} to {}", id, index);
        } catch (IOException | OpenSearchException e) {
            logger.warn("Could not update to index: " + id, e);
        }
    }

    public void removeFromIndexWithQuery(String index, Query query) {
        try {
            final long startTime = System.currentTimeMillis();
            DeleteByQueryRequest req = new DeleteByQueryRequest.Builder()
                    .index(index)
                    .query(query)
                    .refresh(true)
                    .build();
            var response = client.deleteByQuery(req);
            logger.info("Removed {} items from {} (took {} ms)", response.deleted(), index, System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void removeFromIndex(String index,
                                          String id) {
        String encId = DataModelUtils.encode(id);
        try {
            final long startTime = System.currentTimeMillis();
            DeleteRequest req = new DeleteRequest.Builder()
                    .index(index)
                    .id(encId)
                    .refresh(Refresh.WaitFor)
                    .build();
            client.delete(req);
            logger.info("Removed {} from {} (took {} ms)", id, index, System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }


}
