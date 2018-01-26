/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;

/**
 *
 * @author malonen
 */
public class NamespaceResolver {
   
    static final private Logger logger = Logger.getLogger(NamespaceResolver.class.getName());
    	
	public static Boolean resolveNamespace(String namespace, String alternativeURL, boolean force) {
		
 
            try { // Unexpected exception
                
                IRI namespaceIRI = null;
                IRI alternativeIRI = null;
                
                try {
                        IRIFactory iri = IRIFactory.iriImplementation();
                        namespaceIRI = iri.construct(namespace);
                        
                        if(alternativeURL!=null) {
                            alternativeIRI = iri.construct(alternativeURL); 
                       }
                        
                } catch (IRIException e) {
                        logger.warning("Namespace is invalid IRI!");
                        return false;
                }

		if (NamespaceManager.isSchemaInStore(namespace) && !force ) {
			logger.info("Schema found in store: "+namespace);
			return true;
		} else {
			logger.info("Trying to connect to: "+namespace);
                        Model model = ModelFactory.createDefaultModel();

                        URL url;
             
                        try {
                            if(alternativeIRI!=null) {
                                url = new URL(alternativeURL);
                            } else {
                                url = new URL(namespace);
                            }
                        } catch (MalformedURLException e) {
                            logger.warning("Malformed Namespace URL: "+namespace);
                            return false;
                        }
                        
                        if(!("https".equals(url.getProtocol()) || "http".equals(url.getProtocol()))) {
                            logger.warning("Namespace NOT http or https: "+namespace);
                            return false;
                        }
                                
			HttpURLConnection connection = null;
                        
			try { // IOException

				connection = (HttpURLConnection) url.openConnection();				
				// 2,5 seconds
				connection.setConnectTimeout(4000);
				// 2,5 minutes
				connection.setReadTimeout(30000);
				connection.setInstanceFollowRedirects(true);
                                //,text/rdf+n3,application/turtle,application/rdf+n3
				//"application/rdf+xml,application/xml,text/html");
				connection.setRequestProperty("Accept","application/rdf+xml,application/turtle;q=0.8,application/x-turtle;q=0.8,text/turtle;q=0.8,text/rdf+n3;q=0.5,application/n3;q=0.5,text/n3;q=0.5");
				
                                try { // SocketTimeOut
					
                                    connection.connect();

                                    InputStream stream;

                                    try {
                                        stream = connection.getInputStream();
                                    }  catch (IOException e) {
                                            logger.warning("Couldnt read from "+namespace);
                                            return false;
                                    } 

                                    logger.info("Opened connection");
                                    logger.info(connection.getURL().toString());
                                    logger.info(connection.getContentType());

                                    if(connection.getContentType()==null) {
                                            logger.info("Couldnt resolve Content-Type from: "+namespace);
                                            return false;
                                    }

                                    String contentType = connection.getContentType();

                                    if(contentType==null){
                                            logger.info("ContentType is null");
                                            stream.close();
                                            connection.disconnect();
                                            return false;
                                    }
                                    
                                    ContentType guess = ContentType.create(contentType);
                                    Lang testLang = RDFLanguages.contentTypeToLang(guess);
                                    
                                    if(connection.getURL().toString().endsWith(".ttl"))
                                        testLang = RDFLanguages.fileExtToLang("ttl");
                                    
                                    if(connection.getURL().toString().endsWith(".nt"))
                                        testLang = RDFLanguages.fileExtToLang("nt");
                                    
                                    if(connection.getURL().toString().endsWith(".jsonld"))
                                        testLang = RDFLanguages.fileExtToLang("jsonld");
                                    
                                    if(testLang!=null) {

                                        logger.info("Trying to parse "+testLang.getName()+" from "+namespace);

                                        RDFReader reader = model.getReader(testLang.getName());

                                        reader.setProperty("error-mode", "lax");
                                        
                                        try {
                                            logger.info(""+stream.available());
                                             reader.read(model, stream, namespace);
                                        } catch(RiotException e) {
                                             logger.info("Could not read file from "+namespace);
                                            return false;
                                        }

                                        stream.close();
                                        connection.disconnect();

                                    } else {
                                        logger.info("Cound not resolve Content-Type "+contentType+" from "+namespace);
                                        stream.close();
                                        connection.disconnect();
                                        return false;
                                    }
					
					
                            } catch (UnknownHostException e) {
                                logger.warning("Invalid hostname "+namespace);
                                return false;
                            } catch (SocketTimeoutException e) {
                                logger.info("Timeout from "+namespace);
                                e.printStackTrace();
                                return false;
                            } catch (RuntimeIOException e) {
                                logger.info("Could not parse "+namespace);
                                e.printStackTrace();
                                return false;
                            }
				
			} catch (IOException e) {
                            logger.info("Could not read file from "+namespace);
                            return false;
			} 
				
                        logger.info("Model-size is: "+model.size());

                        try {
                                if(model.size()>1) {
                                        NamespaceManager.putSchemaToStore(namespace,model);
                                } else {
                                    logger.warning("Namespace contains empty schema: "+namespace);
                                    return false;
                                }
 
                                return true;
                                
                        } catch(HttpException ex) {
                                logger.warning("Error in saving the model loaded from "+namespace);
                                return false;
                        }


			} 
                
		} catch(Exception ex) {
			logger.warning("Error in loading the "+namespace);
			ex.printStackTrace();
			return false;
		}
		
		
	}
    
    
}
