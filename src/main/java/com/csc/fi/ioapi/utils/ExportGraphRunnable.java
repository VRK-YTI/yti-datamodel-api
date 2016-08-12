/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

/**
 *
 * @author malonen
 */
public class ExportGraphRunnable implements Runnable {

    private final String graph;
    
    ExportGraphRunnable(String graph) {
        this.graph = graph;
    }
    
    @Override
    public void run() {
        GraphManager.createExportGraphInRunnable(graph);
    }
    
}
