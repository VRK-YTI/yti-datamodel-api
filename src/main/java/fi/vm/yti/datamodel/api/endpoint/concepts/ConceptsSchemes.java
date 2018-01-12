/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.jena.rdf.model.Model;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSchemes")
@Api(tags = {"Concept"}, description = "Available concept schemes from Term editor")
public class ConceptsSchemes {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get available concepts", notes = "Lists terminologies from Termeditor")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Concepts"),
      @ApiResponse(code = 406, message = "Term not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response vocab() {

      Model schemeModel = JerseyJsonLDClient.getJSONLDResponseAsJenaModel(JerseyJsonLDClient.getSchemesFromTermedAPI());
      schemeModel.setNsPrefix("skos","http://www.w3.org/2004/02/skos/core#");

      return JerseyResponseManager.okModel(schemeModel);

  }
  
  
}
