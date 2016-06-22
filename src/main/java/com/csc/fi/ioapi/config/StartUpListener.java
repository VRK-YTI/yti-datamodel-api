/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.GroupManager;
import com.csc.fi.ioapi.utils.NamespaceManager;
import com.csc.fi.ioapi.utils.OPHCodeServer;
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
        initDefaultGroups();
        initDefaultNamespaces();
        initCodeServers();
        loadSchemesFromFinto();
        
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
       System.out.println("System is closing ...");
       logger.log(Level.INFO, "System is closing ...");
    }
    
    private static void initCodeServers() {
        
    OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", true);   
    
    if(!codeServer.status) logger.warning("Code server was not initialized!");
    
    }
    
    private static void loadSchemesFromFinto() {
        ConceptMapper.updateSchemesFromFinto();
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
    private static void initDefaultGroups() {
         if(GroupManager.testDefaultGroups()) {
                logger.log(Level.INFO,"Default groups are initialized!");
         }
         else {
                logger.log(Level.WARNING,"Default groups are NOT initialized!");
                
                GroupManager.createDefaultGroups();
                
                if(GroupManager.testDefaultGroups())
                    logger.log(Level.INFO,"Created default groups!");
                else
                    logger.log(Level.WARNING,"Failed to create default groups!");
              }
    }
    
    private static void initDefaultNamespaces() {
        
        NamespaceManager.resolveDefaultNamespaceToTheCore();
        
    }
    
}
