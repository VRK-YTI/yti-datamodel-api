/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.util.UUID;

public class ProvenanceGraphRunnable implements Runnable {
    private final String graph;
    private final String jsonld;
    private final String user;
    private final UUID provUUID;
    
    ProvenanceGraphRunnable(String graph, String jsonld, String user, UUID provUUID) {
        this.graph = graph;
        this.jsonld = jsonld;
        this.user = user;
        this.provUUID = provUUID;
    }
    
    @Override
    public void run() {
        ProvenanceManager.createProvenanceGraphInRunnable(graph, jsonld, user, provUUID);
    }
    
}
