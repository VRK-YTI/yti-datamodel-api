package fi.vm.yti.datamodel.api;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.index.FrameManager;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.datamodel.api.service.TermedTerminologyManager;
import fi.vm.yti.migration.MigrationInitializer;

@Component
public class StartUpListener {

    private static final Logger logger = LoggerFactory.getLogger(StartUpListener.class.getName());

    private final TermedTerminologyManager termedTerminologyManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final GraphManager graphManager;
    private final NamespaceManager namespaceManager;
    private final ElasticConnector elasticConnector;
    private final FrameManager frameManager;
    private final SearchIndexManager searchIndexManager;

    @Autowired
    StartUpListener(TermedTerminologyManager termedTerminologyManager,
                    RHPOrganizationManager rhpOrganizationManager,
                    GraphManager graphManager,
                    NamespaceManager namespaceManager,
                    ElasticConnector elasticConnector,
                    FrameManager frameManager,
                    SearchIndexManager searchIndexManager,
                    MigrationInitializer migrationInitializer /* XXX: dependency to enforce init order */) {

        this.termedTerminologyManager = termedTerminologyManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.graphManager = graphManager;
        this.namespaceManager = namespaceManager;
        this.elasticConnector = elasticConnector;
        this.frameManager = frameManager;
        this.searchIndexManager = searchIndexManager;
    }

    @PostConstruct
    public void contextInitialized() {

        logger.info("System is starting ...");

        initDefaultNamespaces();
        initRHPOrganizations();
        initElasticsearchIndices();
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
            frameManager.cleanCachedFrames(true);
            searchIndexManager.reindex();
        } catch (IOException e) {
            logger.warn("Elasticsearch initialization failed!", e);
        }
    }
}
