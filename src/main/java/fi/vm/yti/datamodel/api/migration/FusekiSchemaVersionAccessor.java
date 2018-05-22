package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.migration.InitializationException;
import fi.vm.yti.migration.SchemaVersionAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FusekiSchemaVersionAccessor implements SchemaVersionAccessor {

    private final GraphManager graphManager;

    @Autowired
    FusekiSchemaVersionAccessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public boolean isInitialized() throws InitializationException {
        // TODO proper implementation
        return graphManager.testDefaultGraph();
    }

    @Override
    public void initialize() {
        // TODO proper implementation
        setSchemaVersion(0);
    }

    @Override
    public int getSchemaVersion() {
        // TODO proper implementation
        return 1;
    }

    @Override
    public void setSchemaVersion(int version) {
        // TODO proper implementation
    }
}
