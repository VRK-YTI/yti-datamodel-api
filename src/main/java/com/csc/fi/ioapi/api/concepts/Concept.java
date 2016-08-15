/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.LDHelper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.uri.UriComponent;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
          @ApiParam(value = "uri", required = true) @QueryParam("uri") String uri) {
  
       IRI conceptIRI = null;
       
	try {
                    IRIFactory iri = IRIFactory.uriImplementation();
                    
                    if(uri!=null && !uri.equals("undefined")) conceptIRI = iri.construct(uri);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "ID is invalid IRI!");
			return Response.status(403).build();
		}
                
          Response.ResponseBuilder rb;
          
          Client client = Client.create();
          String queryString;
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
          queryString = "CONSTRUCT { "
                      + "?concept a skos:Concept . " 
                      + "?concept skos:prefLabel ?label . "
                      + "?concept skos:definition ?definition . "
                      + "?concept skos:inScheme ?scheme . "
                      + "?scheme a skos:ConceptScheme . "
                      + "?scheme dcterms:identifier ?id . "
                      + "?scheme dcterms:title ?title .  "
                      + "?scheme dcterms:description ?description . "
                      + "?scheme dcterms:isFormatOf ?FintoLink . "
                      + "} WHERE { "
                      + "?concept a skos:Concept . "
                      + "?concept skos:prefLabel ?label . "
                      + "OPTIONAL { ?concept skos:definition ?definition . }"
                      + "?concept skos:inScheme ?scheme . "
                      + "?scheme dc:identifier ?id . "
                      + "?scheme dc:title ?title . "
                      + "?scheme dc:description ?description . "
                      + "?scheme dcterms:isFormatOf ?FintoLink . "
                      + "}";

  	  
          pss.setCommandText(queryString);
          
          if(conceptIRI!=null) pss.setIri("concept", conceptIRI);
                
          WebResource webResource = client.resource(services.getTempConceptReadSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

          WebResource.Builder builder = webResource.accept("application/ld+json");

          ClientResponse response = builder.get(ClientResponse.class);
          rb = Response.status(response.getStatus()); 
          rb.entity(response.getEntityInputStream());
            
          return rb.build();


      /*
            ResponseBuilder rb;
            Client client = Client.create();

            if((uri==null || uri.equals("undefined")) ) return Response.status(Response.Status.NOT_ACCEPTABLE).entity("{}").build();
            
            WebResource webResource = client.resource(services.getConceptAPI())
                                      .queryParam("uri", UriComponent.encode(uri,UriComponent.Type.QUERY))
                                      .queryParam("format","application/json");

            Builder builder = webResource.accept("application/json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Concept.class.getName()).log(Level.INFO, response.getStatus()+" from CONCEPT SERVICE");
               return Response.status(response.getStatus()).entity("{}").build();
            }
            
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
       
           return rb.build();
    
*/
          
  }
  
  
}
