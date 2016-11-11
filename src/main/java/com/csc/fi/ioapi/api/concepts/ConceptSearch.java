/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
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
@Path("conceptSearch")
@Api(value = "/conceptSearch", description = "Concepts search from finto")
public class ConceptSearch {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
   
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get available concepts", notes = "Search from finto API & concept temp")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Concepts"),
      @ApiResponse(code = 406, message = "Term not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response concept(
          @ApiParam(value = "Term", required = true) 
          @QueryParam("term") String term,
          @ApiParam(value = "Lang", required = true) 
          @QueryParam("lang") String lang,
          @ApiParam(value = "vocid") 
          @QueryParam("vocid") String vocid) {
            
          //  return JerseyJsonLDClient.getGraphFromTermedAPI(ApplicationProperties.getDefaultTermAPI()+"nodes?query="+term);
  
         
            
            if((term==null || term.equals("undefined")) || (lang==null || lang.equals("undefined")) ) return JerseyResponseManager.invalidParameter();
            
            if(!term.contains("*")) term+="*";
            
            return JerseyJsonLDClient.getSearchResultFromFinto(vocid, term, lang);

          
  }
  
  
}
