/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.utils.*;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("search")
@Api(tags = {"Resource"}, description = "Search resources")
public class Search {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
            
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Sparql query to given service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Query parse error"),
      @ApiResponse(code = 500, message = "Query exception"),
      @ApiResponse(code = 200, message = "OK")
  })
  public Response search(
          @ApiParam(value = "Search in graph") @QueryParam("graph") String graph,
          @ApiParam(value = "Searchstring", required = true) @QueryParam("search") String search,
          @ApiParam(value = "Language") @QueryParam("lang") String lang) {      

      if(graph == null || graph.equals("undefined") || graph.equals("default")) {
          return JerseyResponseManager.okModel(SearchManager.search(null, search, lang));
      } else {
          if(!IDManager.isValidUrl(graph)) {
              return JerseyResponseManager.invalidParameter();
          } else {
              return JerseyResponseManager.okModel(SearchManager.search(graph, search, lang));
          }
      }


  }
  
}
