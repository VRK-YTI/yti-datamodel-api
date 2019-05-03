/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author amiika
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
    protected RestHighLevelClient elasticSearchClient() {
	RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
    			new HttpHost(config.getElasticHost(), Integer.parseInt(config.getElasticHttpPort()),config.getElasticHttpScheme())
    		));
    	return client;
    }
    
}
