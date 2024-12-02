package fi.vm.yti.datamodel.api.v2;

import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.IndexService;
import fi.vm.yti.datamodel.api.v2.service.NamespaceService;
import fi.vm.yti.migration.MigrationConfig;
import fi.vm.yti.migration.MigrationInitializer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.stereotype.Component;

@Component
@ImportAutoConfiguration(MigrationConfig.class)
public class StartUpListener {

    private static final Logger logger = LoggerFactory.getLogger(StartUpListener.class);

    private final GroupManagementService groupManagementService;
    private final IndexService indexService;
    private final CoreRepository coreRepository;
    private final NamespaceService namespaceService;

    @Autowired
    StartUpListener(GroupManagementService groupManagementService,
                    IndexService indexService,
                    NamespaceService namespaceService,
                    MigrationInitializer migrationInitializer,
                    CoreRepository coreRepository) {
        this.groupManagementService = groupManagementService;
        this.indexService = indexService;
        this.namespaceService = namespaceService;
        this.coreRepository = coreRepository;
    }

    @PostConstruct
    public void contextInitialized() {
        logger.info("System is starting ...");

        initOrganizations();
        initUsers();
        initServiceCategories();
        indexService.initIndexes();
        initDefaultNamespaces();
    }

    private void initOrganizations() {
        groupManagementService.initOrganizations();
    }

    private void initServiceCategories() {
        coreRepository.initServiceCategories();
    }

    private void initDefaultNamespaces() {
        namespaceService.resolveDefaultNamespaces();
    }

    private void initUsers() {
        groupManagementService.initUsers();
    }
}
