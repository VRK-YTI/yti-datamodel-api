package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.yti.migration.MigrationTask;

@Component
public class V1_Initial implements MigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(V1_Initial.class.getName());
    private final JenaService jenaService;

    public V1_Initial(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    @Override
    public void migrate() {
        /* TODO: migrations
        logger.debug("Creating default graph and service categories");
        graphManager.createDefaultGraph();
        graphManager.initServiceCategories();

        if (!graphManager.testDefaultGraph()) {
            throw new RuntimeException("Failed to create default graph");
        }
         */
    }
}
