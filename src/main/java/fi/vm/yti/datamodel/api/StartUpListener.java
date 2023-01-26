package fi.vm.yti.datamodel.api;

import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.ElasticIndexer;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.NamespaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class StartUpListener {

    private static final Logger logger = LoggerFactory.getLogger(StartUpListener.class);

    private final GroupManagementService groupManagementService;
    private final ElasticConnector elasticConnector;
    private final ElasticIndexer elasticIndexer;
    private final JenaService jenaService;
    private final NamespaceService namespaceService;

    @Autowired
    StartUpListener(GroupManagementService groupManagementService,
                    ElasticConnector elasticConnector,
                    ElasticIndexer elasticIndexer,
                    JenaService jenaService,
                    NamespaceService namespaceService) {
        this.groupManagementService = groupManagementService;
        this.elasticConnector = elasticConnector;
        this.elasticIndexer = elasticIndexer;
        this.jenaService = jenaService;
        this.namespaceService = namespaceService;
    }

    @PostConstruct
    public void contextInitialized() {
        logger.info("System is starting ...");

        initDefaultNamespaces();
        initOrganizations();
        initServiceCategories();
        initElasticsearchIndices();
        groupManagementService.updateUsers();
    }

    @Scheduled(cron = "0 */5 * * * *")
    void initOrganizations() {
        groupManagementService.initOrganizations();
    }

    private void initServiceCategories() {
        jenaService.initServiceCategories();
    }

    private void initDefaultNamespaces() {
        namespaceService.resolveDefaultNamespaces();
    }

    private void initElasticsearchIndices() {
        try {
            elasticConnector.waitForESNodes();
            elasticIndexer.reindex();
        } catch (Exception e) {
            logger.warn("Elasticsearch initialization failed!", e);
        }
    }
}
