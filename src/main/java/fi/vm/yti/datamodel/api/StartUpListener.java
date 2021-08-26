package fi.vm.yti.datamodel.api;

import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.GroupManagementService;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.migration.MigrationInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class StartUpListener {

    private static final Logger logger = LoggerFactory.getLogger(StartUpListener.class);

    private final RHPOrganizationManager rhpOrganizationManager;
    private final GraphManager graphManager;
    private final NamespaceManager namespaceManager;
    private final ElasticConnector elasticConnector;
    private final SearchIndexManager searchIndexManager;
    private final GroupManagementService groupManagementService;

    @Autowired
    StartUpListener(RHPOrganizationManager rhpOrganizationManager,
                    GraphManager graphManager,
                    NamespaceManager namespaceManager,
                    ElasticConnector elasticConnector,
                    SearchIndexManager searchIndexManager,
                    MigrationInitializer migrationInitializer,
                    GroupManagementService groupManagementService
                    /* XXX: dependency to enforce init order */) {

        this.rhpOrganizationManager = rhpOrganizationManager;
        this.graphManager = graphManager;
        this.namespaceManager = namespaceManager;
        this.elasticConnector = elasticConnector;
        this.searchIndexManager = searchIndexManager;
        this.groupManagementService = groupManagementService;
    }

    @PostConstruct
    public void contextInitialized() {
        logger.info("System is starting ...");

        initDefaultNamespaces();
        initRHPOrganizations();
        initElasticsearchIndices();
        groupManagementService.updateUsers();
    }

    @PreDestroy
    public void contextDestroyed() {
        logger.info("System is closing ...");
    }

    @Scheduled(cron = "0 */5 * * * *")
    void initRHPOrganizations() {
        rhpOrganizationManager.initOrganizationsFromRHP();
    }

    private void initServiceCategories() {
        graphManager.initServiceCategories();
    }

    private void initDefaultNamespaces() {
        namespaceManager.resolveDefaultNamespaceToTheCore();
    }

    private void initElasticsearchIndices() {
        try {
            elasticConnector.waitForESNodes();
            searchIndexManager.reindex();
        } catch (Exception e) {
            logger.warn("Elasticsearch initialization failed!", e);
        }
    }
}
