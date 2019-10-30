package fi.vm.yti.datamodel.api.service;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;

@Service
public class ClientFactory {

    private final SSLContext sslContext;
    private final ApplicationProperties properties;

    @Autowired
    ClientFactory(SSLContext sslContext,
                  ApplicationProperties properties) {
        this.sslContext = sslContext;
        this.properties = properties;
    }

    public Client create() {
        return ClientBuilder.newBuilder()
            .sslContext(sslContext)
            .build();
    }

    public Client createWithLongTimeout() {
        return ClientBuilder.newBuilder()
            .sslContext(sslContext)
            .property(ClientProperties.CONNECT_TIMEOUT, 180000)
            .property(ClientProperties.READ_TIMEOUT, 180000)
            .build();
    }

}
