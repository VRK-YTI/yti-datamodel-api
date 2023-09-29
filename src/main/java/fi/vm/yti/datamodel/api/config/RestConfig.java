package fi.vm.yti.datamodel.api.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;


@Configuration
public class RestConfig {
    public static final String URI_SUOMI_FI = "http://uri.suomi.fi";
    @Value("${defaultGroupManagementAPI}")
    private String defaultGroupManagementUrl;
    private final HttpHeaders defaultHttpHeaders = new HttpHeaders();
    RestConfig() {
        defaultHttpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
    @Bean("groupManagementClient")
    WebClient defaultWebClient() {
        return WebClient.builder()
                .defaultHeaders(headers -> headers.addAll(defaultHttpHeaders))
                .baseUrl(defaultGroupManagementUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create(getConnectionProvider("groupManagement"))
                )
        ).build();
    }

    @Bean("uriResolveClient")
    WebClient uriResolveClient() {
        var size = 20 * 1024 * 1024;
        return WebClient.builder()
                .defaultHeaders(headers -> headers.addAll(defaultHttpHeaders))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build())
                .baseUrl(URI_SUOMI_FI)
                .clientConnector(new ReactorClientHttpConnector(
                                HttpClient.create(getConnectionProvider("uriResolve"))
                                        .followRedirect(true)
                        )
                ).build();
    }

    @NotNull
    private static ConnectionProvider getConnectionProvider(String name) {
        return ConnectionProvider.builder(name)
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();
    }
}
