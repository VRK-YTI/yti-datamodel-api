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

    /**
     * Returns true if url is absolute
     * @param URL as string
     * @return boolean
     */
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

    /**
     * Returns true if url is not absolute
     * @param url
     * @return boolean
     */
    public static boolean isInvalid(String url) {
        return !isValidUrl(url);
    }

    /**
     * Creates IRI from string
     * @param url
     * @return returns created IRI
     * @throws IRIException
     * @throws NullPointerException
     */
    public static IRI constructIRI(String url) throws IRIException, NullPointerException {
        return iriFactory.construct(url);
    }
    
    
}
