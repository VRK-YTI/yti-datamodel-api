/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author jkesanie
 */

@Configuration
public class ElasticConfig {
    
    private final ApplicationProperties config;
    
    @Autowired 
    public ElasticConfig(ApplicationProperties config) {
        this.config = config;
    }
    
    @Bean
    @SuppressWarnings("resource")
    protected Client elasticSearchClient() throws UnknownHostException {
        Settings settings = Settings.builder()
            .put("cluster.name", config.getElasticCluster()).build();
        TransportClient client = new PreBuiltTransportClient(settings)
            .addTransportAddress(new TransportAddress(InetAddress.getByName(config.getElasticHost()), Integer.parseInt(config.getElasticPort())));
        return client;
    } 
    
}
