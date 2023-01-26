package fi.vm.yti.datamodel.api.v2.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.ElasticIndexer;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ElasticIndexer.class
})
public class ElasticIndexerTest {

    @MockBean
    JenaService jenaService;

    @MockBean
    ElasticConnector elasticConnector;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    ModelMapper modelMapper;

    @MockBean
    RestHighLevelClient esClient;

    @Autowired
    ElasticIndexer elasticIndexer;


    @Test
    void initModelIndexTest() throws IOException {
        var model = mock(Model.class);
        var subjects = mock(ResIterator.class);
        when(jenaService.constructWithQuery(any(Query.class))).thenReturn(model);
        when(model.listSubjects()).thenReturn(subjects);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        elasticIndexer.initModelIndex();

        verify(this.jenaService).constructWithQuery(any(Query.class));
    }
}
