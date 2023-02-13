package fi.vm.yti.datamodel.api.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    private final String openSearchHost;
    private final Integer openSearchHttpPort;
    private final String openSearchScheme;

    @Autowired
    public OpenSearchConfig(@Value("${openSearchHost}") String openSearchHost,
                            @Value("${openSearchHttpPort}") String openSearchHttpPort,
                            @Value("${openSearchHttpScheme:http}") String openSearchScheme) {
        this.openSearchHost = openSearchHost;
        this.openSearchHttpPort = Integer.parseInt(openSearchHttpPort);
        this.openSearchScheme = openSearchScheme;
    }

    @Bean
    protected OpenSearchClient openSearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost(openSearchHost, openSearchHttpPort, openSearchScheme)
        ).setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000)
        ).build();

        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
