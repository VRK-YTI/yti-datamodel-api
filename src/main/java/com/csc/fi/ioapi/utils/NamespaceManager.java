/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

/**
 *
 * @author malonen
 */
public class NamespaceManager {

    static EndpointServices services = new EndpointServices();
     
    static boolean isSchemaInStore(String namespace) {
        
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        return adapter.containsModel(namespace);
        
    }

    static void putSchemaToStore(String namespace, Model model) {
        
      	DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getImportsReadWriteAddress());
	DatasetAdapter adapter = new DatasetAdapter(accessor);
	adapter.add(namespace, model);
    
    }   



}
