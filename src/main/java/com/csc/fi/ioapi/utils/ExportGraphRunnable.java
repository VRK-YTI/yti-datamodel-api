/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.util.logging.Logger;

/**
 *
 * @author malonen
 */
public class ExportGraphRunnable implements Runnable {
    private static final Logger logger = Logger.getLogger(ExportGraphRunnable.class.getName());
    private final String graph;
    
    ExportGraphRunnable(String graph) {
        this.graph = graph;
    }
    
    @Override
    public void run() {
        GraphManager.constructExportGraph(graph);
    }
    
}

/* 
 This could be changed to:

public static void createProvenanceGraph(String graph, String jsonld, String user, UUID provUUID) {
        ThreadExecutor.pool.execute(() -> ProvenanceManager.createProvenanceGraphInRunnable(graph, jsonld, user, provUUID));
}


OR 

        ThreadExecutor.pool.execute(() -> {
            logger.info("Creating prov graph "+graph+" "+provUUID.toString());
            ClientResponse response = JerseyFusekiClient.putGraphToTheService("urn:uuid:"+provUUID.toString(), jsonld, services.getProvReadWriteAddress());

            ...
        });

*/

