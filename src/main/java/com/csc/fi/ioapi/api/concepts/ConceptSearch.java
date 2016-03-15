/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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
   
            ResponseBuilder rb;
            Client client = Client.create();
            
            if(!vocid.startsWith(services.getConceptServiceUri()))
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(ErrorMessage.INVALIDVOCABULARY).build();

            if((term==null || term.equals("undefined")) || (lang==null || lang.equals("undefined")) ) return Response.status(Response.Status.NOT_ACCEPTABLE).entity(ErrorMessage.INVALIDPARAMETER).build();
            
            if(!term.contains("*")) term+="*";
            
            String service = services.getConceptSearchAPI();

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("query", term);
            params.add("lang",lang);
            
            if(vocid!=null && !vocid.equals("undefined")) 
               params.add("vocab",vocid);
            
            WebResource webResource = client.resource(service)
                                            .queryParams(params);

            Builder builder = webResource.accept("application/ld+json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ConceptSearch.class.getName()).log(Level.INFO, response.getStatus()+" from CONCEPT SERVICE");
               return Response.status(response.getStatus()).entity("{}").build();
            }
            
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();
    

          
  }
  
  
}
