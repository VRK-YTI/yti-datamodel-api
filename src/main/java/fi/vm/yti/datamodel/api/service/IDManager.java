/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.springframework.stereotype.Service;

@Service
public class IDManager {

    private static final IRIFactory iriFactory = IRIFactory.iriImplementation();

    /**
     * Returns true if url is absolute
     *
     * @param url as string
     * @return boolean
     */
    public boolean isValidUrl(String url) {

        if (url == null || url.isEmpty()) return false;

        try {
            IRI testIRI = iriFactory.construct(url);
            return testIRI.isAbsolute();
        } catch (IRIException e) {
            return false;
        }

    }

    /**
     * Returns true if url is not absolute
     *
     * @param url
     * @return boolean
     */
    public boolean isInvalid(String url) {
        return !isValidUrl(url);
    }

    /**
     * Creates IRI from string
     * TODO: this needs to be somehow replaced by IRIx?
     * @param url
     * @return returns created IRI
     * @throws IRIException
     * @throws NullPointerException
     */
    public IRI constructIRI(String url) throws IRIException, NullPointerException {
        return iriFactory.construct(url);
    }
}
