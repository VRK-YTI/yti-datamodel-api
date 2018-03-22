package fi.vm.yti.datamodel.api;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import fi.vm.yti.datamodel.api.service.FrameManager;
import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.datamodel.api.service.TermedTerminologyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class StartUpListener  {

    private static final Logger logger = Logger.getLogger(StartUpListener.class.getName());

    private final TermedTerminologyManager termedTerminologyManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final GraphManager graphManager;
    private final NamespaceManager namespaceManager;
    private final EndpointServices endpointServices;
    private final FrameManager frameManager;

    @Autowired
    StartUpListener(TermedTerminologyManager termedTerminologyManager,
                    RHPOrganizationManager rhpOrganizationManager,
                    GraphManager graphManager,
                    NamespaceManager namespaceManager,
                    EndpointServices endpointServices,
                    FrameManager frameManager) {

        this.termedTerminologyManager = termedTerminologyManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.graphManager = graphManager;
        this.namespaceManager = namespaceManager;
        this.endpointServices = endpointServices;
        this.frameManager = frameManager;
    }

    @PostConstruct
    public void contextInitialized() {

        System.out.println("System is starting ...");
        logger.log(Level.INFO, "System is starting ...");

        initDefaultGraph();
        initDefaultNamespaces();
        initCodeServers();
        initServiceCategories();
        termedTerminologyManager.initConceptsFromTermed();
        initRHPOrganizations();
        //initFramingCache();
    }

    @PreDestroy
    public void contextDestroyed() {
        System.out.println("System is closing ...");
        logger.log(Level.INFO, "System is closing ...");
    }

    @Scheduled(cron = "0 */5 * * * *")
    void initRHPOrganizations() {
        logger.info("Updating organizations");
        rhpOrganizationManager.initOrganizationsFromRHP();
    }

    private void initServiceCategories() {
        graphManager.initServiceCategories();
    }

    private void initCodeServers() {

        OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", true, endpointServices);
        if(!codeServer.status) logger.warning("Code server was not initialized!");
    }

    private void initDefaultGraph() {
        if (graphManager.testDefaultGraph()) {
            logger.log(Level.INFO,"Default graph is initialized!");
        }
        else {
            logger.log(Level.WARNING,"Default graph is NOT initialized!");
            graphManager.createDefaultGraph();
            if (graphManager.testDefaultGraph())
                logger.log(Level.INFO,"Created NEW DEFAULT graph!");
            else
                logger.log(Level.WARNING,"Failed to create default graph!");
        }
    }

    private void initDefaultNamespaces() {
        namespaceManager.resolveDefaultNamespaceToTheCore();
    }
    
    private void initFramingCache() {
        frameManager.initCache();
    }
}
