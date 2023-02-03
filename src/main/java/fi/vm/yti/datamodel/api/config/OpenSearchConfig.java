package fi.vm.yti.datamodel.api.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
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
    protected RestHighLevelClient openSearchClient() {
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(openSearchHost, openSearchHttpPort, openSearchScheme)
        ).setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000))
        );
    }
}
