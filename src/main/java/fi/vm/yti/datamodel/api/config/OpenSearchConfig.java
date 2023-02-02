/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private final String elasticHost;
    private final Integer elasticHttpPort;
    private final String elasticScheme;

    @Autowired
    public OpenSearchConfig(@Value("${elasticHost}") String elasticHost,
                            @Value("${elasticHttpPort}") String elasticHttpPort,
                            @Value("${elasticHttpScheme:http}") String elasticScheme) {
        this.elasticHost = elasticHost;
        this.elasticHttpPort = Integer.parseInt(elasticHttpPort);
        this.elasticScheme = elasticScheme;
    }

    @Bean
    protected RestHighLevelClient elasticSearchClient() {
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(elasticHost, elasticHttpPort, elasticScheme)
        ).setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000))
        );
    }
}
