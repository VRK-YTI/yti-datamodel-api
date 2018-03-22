/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import static org.mockito.Mockito.*;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author jkesanie
 */
public class FrameManagerTest  {
    
    public FrameManagerTest() {
    }

    @Test
    public void testGetCachedClassVisualizationFrame() {
        System.out.println("cacheClassVisualizationFrame");                
        String id = "http://ex.com/id:2";
        Resource dataFile = new ClassPathResource("test-model.ttl");
        
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(dataFile.getFile().getAbsolutePath());
            System.out.println(dataFile.getFile().getAbsolutePath());
            Client client = mock(Client.class);                        
            IndexRequestBuilder irb = mock(IndexRequestBuilder.class);
            when(client.prepareIndex(any(), any(), any())).thenReturn(irb);
            when(irb.execute()).thenReturn(mock(ActionFuture.class));
            
            FrameManager instance = new FrameManager(client);
            
            
            instance.cacheClassVisualizationFrame(id, model);
            
            verify(client).prepareIndex(FrameManager.ELASTIC_INDEX_MODEL, "doc", "http%3A%2F%2Fex.com%2Fid%3A2");
            ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);
            verify(irb).setSource(sourceCaptor.capture());
            
            String frameJson = sourceCaptor.getValue();
            
            System.out.println(frameJson);
            assertThat(frameJson, isJson());   
            
            assertThat(frameJson, hasJsonPath("$['@graph'][0]['@id']", equalTo("testaa:Test")));
            
            assertThat(frameJson, hasJsonPath("$['@graph'][0]['property']['predicate']", equalTo("testaa:test-property")));
            
            
   
        }catch(Exception ex) {
            ex.printStackTrace();
            fail("Exception was thrown:" + ex.getMessage());
        }
        
    }
    
}
