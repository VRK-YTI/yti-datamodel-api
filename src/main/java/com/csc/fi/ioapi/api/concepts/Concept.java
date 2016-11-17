/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.LDHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.query.ParameterizedSparqlString;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("concept")
@Api(value = "/concept", description = "Get concept with id")
public class Concept {

    @Context ServletContext context;
  private EndpointServices services = new EndpointServices();
  private static final Logger logger = Logger.getLogger(ConceptSuggestion.class.getName());
   
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get available concepts", notes = "Search from finto API & concept temp")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Concepts"),
      @ApiResponse(code = 406, message = "Term not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response concept(
          @ApiParam(value = "uri", required = true) 
          @QueryParam("uri") String uri,
          @ApiParam(value = "vocid") 
          @QueryParam("vocid") String vocid) {

        return JerseyJsonLDClient.getConceptFromTermedAPI(uri,vocid);          
      
  }
  
  
}
