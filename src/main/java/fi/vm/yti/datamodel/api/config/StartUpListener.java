/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.utils.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author malonen
 */
public class StartUpListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(StartUpListener.class.getName());
            
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        System.out.println("System is starting ...");
        logger.log(Level.INFO, "System is starting ...");

        initDefaultGraph();
        initDefaultNamespaces();
        initCodeServers();
        initServiceCategories();
        TermedTerminologyManager.initConceptsFromTermed();

        runPeriodicUpdates();

    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
       System.out.println("System is closing ...");
       logger.log(Level.INFO, "System is closing ...");
    }

    private static void initRHPOrganizations() {
        RHPOrganizationManager.initOrganizationsFromRHP();
    }

    private static void initServiceCategories() {
        GraphManager.initServiceCategories();
    }

    private static void initCodeServers() {
       
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", true);
            if(!codeServer.status) logger.warning("Code server was not initialized!");

    }
    
    private static void runPeriodicUpdates() {

        Runnable runUpdates = () -> {
            logger.info("Updating organizations");
            initRHPOrganizations();
        };

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runUpdates, 0, 5, TimeUnit.MINUTES);

    }

    private static void initDefaultGraph() {
         if(GraphManager.testDefaultGraph()) {
                logger.log(Level.INFO,"Default graph is initialized!");
         }
         else {
                logger.log(Level.WARNING,"Default graph is NOT initialized!");
                GraphManager.createDefaultGraph();
                if(GraphManager.testDefaultGraph())
                    logger.log(Level.INFO,"Created NEW DEFAULT graph!");
                else
                    logger.log(Level.WARNING,"Failed to create default graph!");
              }
    }

    
    private static void initDefaultNamespaces() {
        
        NamespaceManager.resolveDefaultNamespaceToTheCore();
        
    }
    
}
