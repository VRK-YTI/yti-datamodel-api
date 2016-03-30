/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.QueryLibrary;
import org.apache.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("listNamespaces")
@Api(value = "/listNamespaces", description = "Get list of available namespaces")
public class RequiredNamespaces {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(RequiredNamespaces.class.getName());
   
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get available namespaces from service", notes = "Local model namespaces and technical namespaces")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json() {

          String queryString = QueryLibrary.modelQuery;
          
          ParameterizedSparqlString pss = new ParameterizedSparqlString();

             pss.setNsPrefixes(LDHelper.PREFIX_MAP);
             /* IF ID is null or default and no group available */
             queryString = "CONSTRUCT { "
                     + "?g a ?type . "
                     + "?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . "
                     + "?defNS rdfs:label ?nsLabel . "
                     + "?defNS dcap:preferredXMLNamespaceName ?nsNamespace . "
                     + "?defNS dcap:preferredXMLNamespacePrefix ?nsPrefix . "
                     + "} "
                     + "WHERE { "
                     + "GRAPH ?g { "
                     + "?g a ?type . "
                     + "?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . }"
                     + "GRAPH <urn:csc:iow:namespaces> {"
                     + "?defNS rdfs:label ?nsLabel . "
                     + "?defNS dcap:preferredXMLNamespaceName ?nsNamespace . "
                     + "?defNS dcap:preferredXMLNamespacePrefix ?nsPrefix . }"
                     + "}"; 
           
            pss.setCommandText(queryString);
            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

  }
   
}
