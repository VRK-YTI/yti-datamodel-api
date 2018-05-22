package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.model.AbstractClass;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.migration.InitializationException;
import fi.vm.yti.migration.SchemaVersionAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FusekiSchemaVersionAccessor implements SchemaVersionAccessor {

    private final GraphManager graphManager;
    private static final Logger logger = LoggerFactory.getLogger(FusekiSchemaVersionAccessor.class.getName());

    @Autowired
    FusekiSchemaVersionAccessor(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public boolean isInitialized() throws InitializationException {
        return graphManager.isVersionGraphInitialized();
    }

    @Override
    public void initialize() {
        logger.debug("Setting metamodel version to 0");
        setSchemaVersion(0);
    }

    @Override
    public int getSchemaVersion() {
        return graphManager.getVersionNumber();
    }

    @Override
    public void setSchemaVersion(int version) {
        graphManager.setVersionNumber(version);
    }
}
