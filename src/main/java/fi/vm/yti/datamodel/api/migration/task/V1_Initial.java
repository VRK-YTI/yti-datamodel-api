package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S101")
@Component
public class V1_Initial implements MigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(V1_Initial.class.getName());

    @Override
    public void migrate() {

        logger.info("v1");
        /*
        graphManager.createDefaultGraph();
        graphManager.initServiceCategories();

        if (!graphManager.testDefaultGraph()) {
            throw new RuntimeException("Failed to create default graph");
        }
         */
    }
}
