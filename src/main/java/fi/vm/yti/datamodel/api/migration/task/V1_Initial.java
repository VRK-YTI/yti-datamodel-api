package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class V1_Initial implements MigrationTask {

    private final GraphManager graphManager;

    @Autowired
    V1_Initial(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    @Override
    public void migrate() {

        graphManager.createDefaultGraph();

        if (!graphManager.testDefaultGraph()) {
            throw new RuntimeException("Failed to create default graph");
        }
    }
}
