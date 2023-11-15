package fi.vm.yti.datamodel.api.v2.opensearch;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.endpoint.error.OpenSearchException;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.SearchResponseDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexBase;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OpenSearchClientWrapper {

    private final OpenSearchClient client;

    public OpenSearchClientWrapper(OpenSearchConnector connector) {
        this.client = connector.getClient();
    }

    public <T extends IndexBase> SearchResponse<T> searchResponse(SearchRequest request, Class<T> type) {
        try {
            return client.search(request, type);
        } catch (IOException e) {
            throw new OpenSearchException(e.getMessage(), String.join(", ", request.index()));
        }
    }

    public <T extends IndexBase> SearchResponseDTO<T> search(SearchRequest request, Class<T> type) {
        var response = new SearchResponseDTO<T>();
        try {
            var result = client.search(request, type);
            response.setTotalHitCount(result.hits().total().value());
            response.setResponseObjects(result.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(Hit::source)
                    .toList());
            response.setPageFrom(request.from());
            response.setPageSize(request.size());
            return response;
        } catch (IOException e) {
            throw new OpenSearchException(e.getMessage(), String.join(", ", request.index()));
        }
    }
}
