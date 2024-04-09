package fi.vm.yti.datamodel.api.index;

import jakarta.json.stream.JsonGenerator;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.ByteArrayOutputStream;

public class OpenSearchUtils {

    private static final JsonpMapper MAPPER = new JacksonJsonpMapper();

    /**
     * Serialize object to JSON
     * @param object object to serialize
     * @return object as JSON string
     */
    public static String getPayload(JsonpSerializable object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator generator = MAPPER.jsonProvider().createGenerator(out);
        MAPPER.serialize(object, generator);
        generator.close();
        return out.toString();
    }

    /**
     * Add mandatory information to OpenSearch response
     * @return SearchResponse builder
     * @param <T> type of response data
     */
    public static <T> SearchResponse.Builder<T> getBaseResponse() {
        return new SearchResponse.Builder<T>()
                .took(1)
                .timedOut(false)
                .shards(new ShardStatistics.Builder()
                        .failed(0)
                        .successful(1)
                        .total(1)
                        .skipped(0)
                        .build()
                );
    }
}
