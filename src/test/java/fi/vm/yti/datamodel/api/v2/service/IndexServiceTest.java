package fi.vm.yti.datamodel.api.v2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        IndexService.class
})
class IndexServiceTest {

    @MockBean
    CoreRepository coreRepository;

    @MockBean
    ImportsRepository importsRepository;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    ModelMapper modelMapper;

    @MockBean
    DataModelAuthorizationManager authorizationManager;

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    OpenSearchClientWrapper client;

    @Autowired
    IndexService indexService;

    @Test
    void initModelIndexTest() {
        var model = mock(Model.class);
        var subjects = mock(ResIterator.class);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(model);
        when(model.listSubjects()).thenReturn(subjects);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        indexService.initModelIndex();

        verify(coreRepository).queryConstruct(any(Query.class));
    }

    @Test
    void initResourceIndexTest() {
        var model = mock(Model.class);
        var subjects = mock(ResIterator.class);
        when(coreRepository.queryConstruct(any(Query.class))).thenReturn(model);
        when(model.listSubjects()).thenReturn(subjects);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        indexService.initResourceIndex();

        verify(coreRepository).querySelect(any(Query.class), any(Consumer.class));
    }
}
