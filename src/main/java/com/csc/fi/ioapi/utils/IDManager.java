/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.util.logging.Logger;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;

/**
 *
 * @author malonen
 */
public class IDManager {
    
    private static final Logger logger = Logger.getLogger(IDManager.class.getName());
    private static final IRIFactory iriFactory = IRIFactory.iriImplementation();
    
    public static boolean isValidUrl(String url) {
        
        if(url==null || url.isEmpty()) return false;
        
        try {
	    IRI testIRI = iriFactory.construct(url);
            if(testIRI.isAbsolute()) {
                return true;
            }
            else {
                return false;
            }
	} catch (IRIException e) {
            return false;
        }
        
    }
    
    public static boolean isInvalid(String url) {
        return !isValidUrl(url);
    }
    
    public static IRI constructIRI(String url) throws IRIException, NullPointerException {
        return iriFactory.construct(url);
    }
    
    
}
