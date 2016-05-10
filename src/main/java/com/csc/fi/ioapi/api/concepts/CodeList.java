/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.query.ParameterizedSparqlString;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("codeList")
@Api(value = "/codeList", description = "Get list of codes from code sercer")
public class CodeList {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(CodeList.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get list of codelists from code server", notes = "Groups and codeLists")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Codelists"),
      @ApiResponse(code = 406, message = "Term not defined"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response codeList(
          @ApiParam(value = "Codeserver uri", required = true) @QueryParam("uri") String uri) throws IOException {
   /*
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?codeGroup dcterms:hasPart ?codeList . "
                + "?codeGroup dcterms:title ?groupTitle . "
                + "?codeGroup a ?groupType . "
                + "?codeList dcterms:identifier ?id . "
                + "?codeList dcterms:title ?title . "
                + "?codeList dcterms:creator ?creator . "
                + "?codeList a ?codeType . "
                + "} WHERE { "
                + "GRAPH ?codeServer {"
                 + "?codeGroup dcterms:hasPart ?codeList . "
                + "?codeGroup dcterms:title ?groupTitle . "
                + "?codeGroup a ?groupType . "
                + "?codeList dcterms:identifier ?id . "
                + "?codeList dcterms:title ?title . "
                + "OPTIONAL {?codeList dcterms:creator ?creator . }"
                + "?codeList a ?codeType . "
                + "}}"; 
        
        pss.setIri("codeServer", uri);
        pss.setCommandText(queryString);

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getSchemesSparqlAddress()); */
   
        /* Use graph protocol instead for now ... */
        return JerseyFusekiClient.getGraphResponseFromService(uri, services.getSchemesReadAddress());

          
  }
  
  
}
