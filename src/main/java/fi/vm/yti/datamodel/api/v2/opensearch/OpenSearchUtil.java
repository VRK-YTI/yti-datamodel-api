package fi.vm.yti.datamodel.api.v2.opensearch;

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

public class OpenSearchUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchUtil.class);
    private static final JsonpMapper MAPPER = new JacksonJsonpMapper();

    private OpenSearchUtil() {}

    /**
     * Logs payload sent to OpenSearch
     *
     * @param object object
     */
    public static void logPayload(JsonpSerializable object, String index) {
        if (LOG.isDebugEnabled()) {
            var out = new ByteArrayOutputStream();
            var generator = MAPPER.jsonProvider().createGenerator(out);
            MAPPER.serialize(object, generator);
            generator.close();
            LOG.debug("Payload for object of type {} in index {}", object.getClass().getSimpleName(), index);
            LOG.debug(out.toString());
        }
    }
}
