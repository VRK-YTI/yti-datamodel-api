/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("listNamespaces")
@Api(tags = {"Model"}, description = "Get list of available namespaces")
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
                     + "} "
                     + "WHERE { {"
                     + "GRAPH ?g { "
                     + "?g a ?type . "
                     + "?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . } } UNION {"
                     + "GRAPH <urn:csc:iow:namespaces> {"
                     + "?g a ?type . "
                     + "?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . }"
                     + "}}"; 
           
            pss.setCommandText(queryString);
            
            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

  }
   
}
