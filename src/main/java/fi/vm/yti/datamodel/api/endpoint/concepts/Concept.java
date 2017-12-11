/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import fi.vm.yti.datamodel.api.utils.TermedTerminologyManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.jena.query.ParameterizedSparqlString;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("concept")
@Api(tags = {"Concept"}, description = "Get concept with id")
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
          @ApiParam(value = "uri") 
          @QueryParam("uri") String uri,
          @ApiParam(value = "schemeUUID") 
          @QueryParam("schemeUUID") String schemeUUID) {

          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          pss.setNsPrefixes(LDHelper.PREFIX_MAP);
          if(uri!=null && !uri.equals("undefined")) pss.setIri("concept",uri);
          if(schemeUUID!=null && !schemeUUID.equals("undefined")) pss.setLiteral("schemeUUID",schemeUUID);
          pss.setCommandText(QueryLibrary.conceptQuery);
          logger.info(pss.toString());

          return JerseyJsonLDClient.constructResponseFromGraph(TermedTerminologyManager.constructCleanedModelFromTermedAPI(uri, pss.toString()));
      
  }
   
}
