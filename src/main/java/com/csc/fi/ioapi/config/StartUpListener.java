/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author malonen
 */
public class StartUpListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("System is starting ...");
        Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO, "System is starting ...");
        
        
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
       System.out.println("System is closing ...");
       Logger.getLogger(StartUpListener.class.getName()).log(Level.INFO, "System is closing ...");
    }
    
}
