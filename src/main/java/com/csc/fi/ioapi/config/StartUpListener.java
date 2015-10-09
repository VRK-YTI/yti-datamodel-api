/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import com.csc.fi.ioapi.utils.GraphManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 *
 * @author malonen
 */
public class StartUpListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("System is starting ...");
        Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO, "System is starting ...");
        
          if(GraphManager.testDefaultGraph())
                Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO,"Default graph is initialized!");
              else {
                Logger.getLogger(StartUpListener.class.getName()).log(Level.WARNING,"Default graph is NOT initialized!");
                GraphManager.createDefaultGraph();
                if(GraphManager.testDefaultGraph())
                    Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO,"Created NEW DEFAULT graph!");
                else
                    Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO,"Failed to create default graph!");
              }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
       System.out.println("System is closing ...");
       Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO, "System is closing ...");
    }
    
}
