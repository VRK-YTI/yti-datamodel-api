/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.genericapi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("search")
@Api(value = "/search", description = "Search resources")
public class Search {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
            
  @GET
  @Consumes("application/sparql-query")
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

      /* TODO: ADD TEXTDATASET ONCE NAMESPACE BUG IS RESOLVED */
      // if(!search.endsWith("~")||!search.endsWith("*")) search = search+"*";
      
            String queryString = 
                    "CONSTRUCT {"
                  + "?resource rdf:type ?type ."
                  + "?resource rdfs:label ?label ."
                  + "?resource rdfs:comment ?comment ."
                  + "?resource rdfs:isDefinedBy ?super . "
                  + "?super rdfs:label ?superLabel . "
                  + "?super dcap:preferredXMLNamespaceName ?namespace . "
                  + "?super dcap:preferredXMLNamespacePrefix ?prefix . "
                  + "} WHERE { "
                  + (graph==null||graph.equals("undefined")||graph.equals("default")?"":"GRAPH <"+graph+"#HasPartGraph> { <"+graph+"> dcterms:hasPart ?graph . } ")
                  + "GRAPH ?graph {"
                  + "?resource ?p ?literal . "
                  + "FILTER contains(lcase(?literal),lcase(?search)) " 
                  + "?resource rdf:type ?type . "
                  + "OPTIONAL {"
                  + "?resource rdfs:isDefinedBy ?super . "
                  + "GRAPH ?super { ?super rdfs:label ?superLabel . "
                  + "?super dcap:preferredXMLNamespaceName ?namespace . "
                  + "?super dcap:preferredXMLNamespacePrefix ?prefix . "
                  + "}}"
                  //+ "UNION"
                 // + "{?resource sh:predicate ?predicate . ?super sh:property ?resource . ?super rdfs:label ?superLabel . BIND(sh:Constraint as ?type)}"
                  + "?resource rdfs:label ?label . " 
                  //+ "?resource text:query '"+search+"' . "
                  + "OPTIONAL{?resource rdfs:comment ?comment .}"
                  + (lang==null||lang.equals("undefined")?"":"FILTER langMatches(lang(?label),'"+lang+"')")
                  + "}}"; 

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            pss.setLiteral("search", search);
            pss.setCommandText(queryString);
            
            Logger.getLogger(Search.class.getName()).log(Level.INFO, "Searching "+graph+" with query: "+queryString);

            ResponseBuilder rb;
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());

           return rb.build();    

  }
  
}
