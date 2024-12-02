package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.common.Constants;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.migration.task.V10_RenameURIs;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        V10_RenameURIs.class
})
class GraphNameMigrationTest {

    @MockBean
    CoreRepository coreRepository;

    @Captor
    ArgumentCaptor<Model> captor;

    @Autowired
    V10_RenameURIs uriMigration;

    static final String OLD_NS = "http://uri.suomi.fi/datamodel/ns/";
    static final String OLD_POSITION_URI = "http://uri.suomi.fi/datamodel/positions/";

    @Test
    void moveDraftGraphs() {
        var oldGraphURI = OLD_NS + "test";

        var oldData = MapperTestUtils.getModelFromFile("/migration/old-identifiers.ttl");

        mockConsumer(oldGraphURI);
        when(coreRepository.fetch(oldGraphURI)).thenReturn(oldData);

        uriMigration.migrate();

        verify(coreRepository).put(eq(Constants.DATA_MODEL_NAMESPACE + "test/"), captor.capture());
        verify(coreRepository).delete(oldGraphURI);

        // there should not exist any subject, predicate or object in the model containing old namespace
        var model = captor.getValue();
        var containsOldNS = model.listStatements().toList().stream().noneMatch(s ->
                s.getSubject().toString().contains(OLD_NS)
                        || !s.getPredicate().toString().contains(OLD_NS)
                        || !s.getObject().toString().contains(OLD_NS));
        // model resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/").listProperties().hasNext());
        // class resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass").listProperties().hasNext());
        // Attribute resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction").listProperties().hasNext());
        // No resources with old namespace
        assertFalse(containsOldNS);
    }

    @Test
    void moveVersionGraphs() {
        var oldGraphURI = OLD_NS + "test/1.0.0";

        var oldData = MapperTestUtils.getModelFromFile("/migration/old-identifiers-version.ttl");

        mockConsumer(oldGraphURI);
        when(coreRepository.fetch(oldGraphURI)).thenReturn(oldData);

        uriMigration.migrate();

        verify(coreRepository).put(eq(Constants.DATA_MODEL_NAMESPACE + "test/1.0.0/"), captor.capture());
        verify(coreRepository).delete(oldGraphURI);

        // there should not exist any subject, predicate or object in the model containing old namespace
        var model = captor.getValue();
        var containsOldNS = model.listStatements().toList().stream().noneMatch(s ->
                s.getSubject().toString().contains(OLD_NS)
                        || !s.getPredicate().toString().contains(OLD_NS)
                        || !s.getObject().toString().contains(OLD_NS));

        // model resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/").listProperties().hasNext());
        // class resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestClass").listProperties().hasNext());
        // Attribute resource
        assertTrue(model.getResource(Constants.DATA_MODEL_NAMESPACE + "test/TestAttributeRestriction").listProperties().hasNext());
        // No resources with old namespace
        assertFalse(containsOldNS);
    }

    @Test
    void movePositions() {
        var oldPositionURI = OLD_POSITION_URI + "test";
        var oldPositions = MapperTestUtils.getModelFromFile("/migration/old-positions.ttl");

        when(coreRepository.fetch(oldPositionURI)).thenReturn(oldPositions);
        mockConsumer(oldPositionURI);

        uriMigration.migrate();

        verify(coreRepository).put(eq(ModelConstants.MODEL_POSITIONS_NAMESPACE + "test/"), captor.capture());
        verify(coreRepository).delete(oldPositionURI);
    }

    private void mockConsumer(String oldPositionURI) {
        var solution = mock(QuerySolution.class);
        var redNode = mock(RDFNode.class);
        when(redNode.toString()).thenReturn(oldPositionURI);
        when(solution.get(anyString())).thenReturn(redNode);

        doAnswer(a -> {
            var arg = a.getArgument(1, Consumer.class);
            arg.accept(solution);
            return null;
        }).when(coreRepository).querySelect(anyString(), any(Consumer.class));
    }
}
