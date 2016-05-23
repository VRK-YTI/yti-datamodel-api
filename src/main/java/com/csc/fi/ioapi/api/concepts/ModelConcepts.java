/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.concepts;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
 
/**
 * Root resource (exposed at "class" path)
 */
@Path("modelConcepts")
@Api(value = "/modelConcepts", description = "Local concept operations")
public class ModelConcepts {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ModelConcepts.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get used concepts from model", notes = "Get used concepts in JSON-LD")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "No such resource"), 
     @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
      @ApiParam(value = "Concept id")
      @QueryParam("id") String id,
      @ApiParam(value = "Model id", required = true)
      @QueryParam("model") String model) {

        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI;   
        
        /* Check that URIs are valid */
        try {
            
            modelIRI = iriFactory.construct(model);
            if(id!=null && !id.equals("undefined")) idIRI = iriFactory.construct(id);
            
        }
        catch(NullPointerException e) {
            return Response.status(403).entity(ErrorMessage.UNEXPECTED).build();
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
     
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?skosCollection skos:member ?concept . "
                + "?skosCollection a skos:Collection . "
                + "?concept skos:inScheme ?scheme . "
                + "?concept skos:broader ?top . "
                + "?concept a ?type . "
                + "?concept skos:prefLabel ?label . "
                + "?concept skos:definition ?comment . "
                + "?concept prov:generatedAtTime ?time . "
                + "?concept prov:wasAssociatedWith ?user . "
                + "?concept rdfs:isDefinedBy ?model . }"
                + " WHERE { "
                + "GRAPH ?skosCollection {"
                + "?skosCollection skos:member ?concept . "
                + "}"
                + "GRAPH ?concept { "
                + "?concept skos:inScheme ?scheme . "
                + "OPTIONAL { ?concept skos:broader ?top . }"
                + "?concept a ?type . "
                + "?concept skos:prefLabel ?label . "
                + "OPTIONAL { ?concept skos:definition ?comment . }"
                    + "OPTIONAL { "
                    + "?concept prov:generatedAtTime ?time . "
                    + "?concept prov:wasAssociatedWith ?user . "
                    + "}"
                + "OPTIONAL { ?concept rdfs:isDefinedBy ?model . }"
                + "}"
                + "}";

         pss.setIri("skosCollection", model+"/skos#");

         if(id!=null && !id.equals("undefined")) {
             pss.setIri("concept",id);
         }

        
        pss.setCommandText(queryString);
        logger.info(pss.toString());

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());
     
  }
 
  @PUT
  @ApiOperation(value = "PUT existing concept reference to model", notes = "Adds concept reference to model concetps")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "No such resource"),
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response putConceptToModel(
      @ApiParam(value = "Concept id", required = true)
      @QueryParam("id") String id,
      @ApiParam(value = "Model id", required = true)
      @QueryParam("model") String model) {
  
      IRIFactory iriFactory = IRIFactory.iriImplementation();
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
      
      ConceptMapper.addConceptToLocalSKOSCollection(model,id);
  
       return Response.status(200).entity("{}").build();
  }
  
  @DELETE
  @ApiOperation(value = "Delete concept reference from model", notes = "Delete concept reference from model")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "No such resource"),
      @ApiResponse(code = 400, message = "Cannot be removed"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response deleteConceptFromModel(
      @ApiParam(value = "Concept id", required = true)
      @QueryParam("id") String id,
      @ApiParam(value = "Model id", required = true)
      @QueryParam("model") String model,
      @Context HttpServletRequest request) {
  
      IRIFactory iriFactory = IRIFactory.iriImplementation();
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
        
       HttpSession session = request.getSession();

       if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
      
      if(ConceptMapper.deleteModelReference(model,id))
         return Response.status(200).entity("{}").build();
      else
         return Response.status(400).entity(ErrorMessage.USEDIRI).build();
  }
  
}
