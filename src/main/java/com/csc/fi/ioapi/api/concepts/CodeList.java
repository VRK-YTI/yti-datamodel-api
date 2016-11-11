/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.IOException;
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
        return JerseyJsonLDClient.getGraphResponseFromService(uri, services.getSchemesReadAddress());

          
  }
  
  
}
