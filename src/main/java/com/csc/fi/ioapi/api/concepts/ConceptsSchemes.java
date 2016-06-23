/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.apache.jena.query.ParameterizedSparqlString;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSchemes")
@Api(value = "/conceptSchemes", description = "Available concept schemes in Finto")
public class ConceptsSchemes {

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
  public Response vocab() {
          // return JerseyFusekiClient.getGraphResponseFromService("urn:csc:schemes", services.getTempConceptReadWriteAddress());
  
          Response.ResponseBuilder rb;
          
          Client client = Client.create();
          String queryString;
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
          queryString = "CONSTRUCT { "
                      + "?scheme a skos:ConceptScheme . "
                      + "?scheme dcterms:identifier ?id . "
                      + "?scheme rdfs:label ?title .  "
                      + "?scheme rdfs:comment ?description . "
                      + "?scheme dcterms:isFormatOf ?FintoLink . "
                      + "} WHERE { "
                      + "?concept a skos:Concept . "
                      + "?concept skos:prefLabel ?label . "
                      + "?concept skos:definition ?definition . "
                      + "?concept skos:inScheme ?scheme . "
                      + "?scheme dc:identifier ?id . "
                      + "?scheme dc:title ?title . "
                      + "?scheme dc:description ?description . "
                      + "?scheme dcterms:isFormatOf ?FintoLink . "
                      + "}";

  	  
          pss.setCommandText(queryString);
                
          WebResource webResource = client.resource(services.getTempConceptReadSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

          WebResource.Builder builder = webResource.accept("application/ld+json");

          ClientResponse response = builder.get(ClientResponse.class);
          rb = Response.status(response.getStatus()); 
          rb.entity(response.getEntityInputStream());
            
          return rb.build();
  }
  
  
}
