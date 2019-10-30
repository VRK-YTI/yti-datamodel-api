package fi.vm.yti.datamodel.api.migration.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.migration.MigrationTask;

@Component
public class V1_Initial implements MigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(V1_Initial.class.getName());
    private final GraphManager graphManager;

    @Autowired
    V1_Initial(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public void migrate() {
        logger.debug("Creating default graph and service categories");
        graphManager.createDefaultGraph();
        graphManager.initServiceCategories();

        if (!graphManager.testDefaultGraph()) {
            throw new RuntimeException("Failed to create default graph");
        }
    }
}
