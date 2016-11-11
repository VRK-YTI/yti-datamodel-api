/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSuggestion")
@Api(value = "/conceptSuggestion", description = "Edit resources")
public class ConceptSuggestion {

  @Context ServletContext context;
  private EndpointServices services = new EndpointServices();
  private static final Logger logger = Logger.getLogger(ConceptSuggestion.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get concept suggestion", notes = "Get concept suggestions with concept ID, scheme ID or user ID. Concept suggestions with Scheme ID should be most useful. ")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
	@ApiParam(value = "Concept ID") @QueryParam("conceptID") String conceptID,
        @ApiParam(value = "User ID") @QueryParam("userID") String userID,
        @ApiParam(value = "Scheme ID") @QueryParam("schemeID") String schemeID) {

        IRI conceptIRI = null;
        IRI userIRI = null;
        IRI schemeIRI = null;
       
	try {
                    IRIFactory iri = IRIFactory.iriImplementation();
                    
                    if(conceptID!=null && !conceptID.equals("undefined")) conceptIRI = iri.construct(conceptID);
                    if(userID!=null && !userID.equals("undefined")) userIRI = iri.construct(userID);
                    if(schemeID!=null && !schemeID.equals("undefined")) schemeIRI = iri.construct(schemeID);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "ID is invalid IRI!");
			return JerseyResponseManager.invalidIRI();
		}
                
          String queryString;
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
          queryString = "CONSTRUCT { "
                  + "?concept a ?type . "
                  + "?concept skos:inScheme ?scheme . "
                  + "?scheme a ?schemeType . "
                  + "?scheme dcterms:identifier ?schemeID . "
                  + "?scheme dcterms:title ?schemeTitle . "
                  + "?concept rdfs:isDefinedBy ?model . "
                  + "?concept skos:broader ?top . "
                  + "?concept prov:generatedAtTime ?time . "
                  + "?concept skos:prefLabel ?label . "
                  + "?concept skos:definition ?comment . "
                  + "?concept prov:wasAssociatedWith ?user . "
                  + "?concept rdfs:isDefinedBy ?model . "
                  + "?model a ?modelType . "
                  + "?model rdfs:label ?modelLabel . } "
                  + "WHERE {"
                  + "GRAPH ?concept { "
                  + "?concept a ?type . "
                  + "?concept skos:inScheme ?scheme . "
                  + "?concept skos:prefLabel ?label . "
                  + "?concept prov:generatedAtTime ?time . "
                  + "?concept skos:definition ?comment . "
                  + "?concept prov:wasAssociatedWith ?user . "
                  + "OPTIONAL { ?concept skos:broader ?top . }"
                  + "OPTIONAL { ?concept rdfs:isDefinedBy ?model . "
                  + "SERVICE ?modelService { "
                        + "GRAPH ?model {"
                        + " ?model a ?modelType . "
                        + " ?model rdfs:label ?modelLabel . "
                        + " ?model dcterms:references ?scheme . "
                        + " ?scheme a ?schemeType . "
                        + " ?scheme dcterms:identifier ?schemeID . "
                        + " ?scheme dcterms:title ?schemeTitle . "
                        + "}}}"
                  + "}}";
  	  
          pss.setCommandText(queryString);
          
          if(conceptIRI!=null) pss.setIri("concept", conceptIRI);
          if(userIRI!=null) pss.setIri("user", userIRI);
          if(schemeIRI!=null) pss.setIri("scheme", schemeIRI);
          pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
                
          return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());
                  
           
  }
 

    	@PUT
	@ApiOperation(value = "Create concept suggestion", notes = "Create new concept suggestion")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "New concept is created"),
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                        @ApiResponse(code = 401, message = "User is not logged in"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response newConceptSuggestion(
                @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
                @ApiParam(value = "Scheme ID", required = true) @QueryParam("schemeID") String schemeID,
                @ApiParam(value = "Label", required = true) @QueryParam("label") String label,
		@ApiParam(value = "Comment", required = true) @QueryParam("comment") String comment,
                @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
                @ApiParam(value = "Broader concept ID") @QueryParam("topConceptID") String topConceptID,
                @Context HttpServletRequest request) {

                HttpSession session = request.getSession();

                if(session==null) return JerseyResponseManager.unauthorized();

                LoginSession login = new LoginSession(session);

                if(!login.isLoggedIn()) return JerseyResponseManager.unauthorized();
            
		IRI schemeIRI;
                IRI modelIRI;
                IRI topIRI = null;
                
                
		try {
			IRIFactory iri = IRIFactory.iriImplementation();
			schemeIRI = iri.construct(schemeID);
                        modelIRI = iri.construct(modelID);
                        if(topConceptID!=null && !topConceptID.equals("undefined")) topIRI = iri.construct(topConceptID);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return JerseyResponseManager.invalidIRI();
		} catch(NullPointerException e) {
                    	logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return JerseyResponseManager.invalidIRI();
                }
                
                UUID conceptUUID = UUID.randomUUID();
                
		String queryString;
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		
                pss.setNsPrefixes(LDHelper.PREFIX_MAP);
		
                queryString = "INSERT { "
                        + "GRAPH ?concept { "
                        + "?concept rdfs:isDefinedBy ?model . "
                        + "?model a ?modelType ."
                        + "?model rdfs:label ?modelLabel . "
                        + "?concept skos:inScheme ?scheme . "
                        + " ?scheme dcterms:title ?schemeTitle ."
                        + " ?scheme dcterms:identifier ?schemeIdentifier . "
                        + " ?scheme a ?schemeType . "
                        + "?concept skos:broader ?top . "
                        + "?concept a skos:Concept . "
                        + "?concept a iow:ConceptSuggestion . "
                        + "?concept skos:prefLabel ?label . "
                        + "?concept skos:definition ?comment . "
                        + "?concept prov:generatedAtTime ?time . "
                        + "?concept prov:wasAssociatedWith ?user . } } "
                        + "WHERE { BIND(NOW() as ?time)"
                        + "SERVICE ?modelService { "
                        + "GRAPH ?model {"
                        + " ?model dcterms:references ?scheme . "
                        + " ?scheme dcterms:title ?schemeTitle ."
                        + " ?scheme dcterms:identifier ?schemeIdentifier . "
                        + " ?scheme a ?schemeType . "
                        + " ?model a ?modelType . "
                        + " ?model rdfs:label ?modelLabel . "
                        + "}}"
                        + "}";
		
                pss.setCommandText(queryString);
                pss.setIri("model",modelIRI);
		pss.setIri("scheme", schemeIRI);
                pss.setLiteral("label", ResourceFactory.createLangLiteral(label,lang));
                pss.setLiteral("comment", ResourceFactory.createLangLiteral(comment,lang));
                pss.setIri("concept", "urn:uuid:"+conceptUUID);
                pss.setIri("user", "mailto:"+login.getEmail());        
                pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
          
                if(topIRI!=null) pss.setIri("top", topIRI);
         
		UpdateRequest query = pss.asUpdate();
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(query, services.getTempConceptSparqlUpdateAddress());
                ConceptMapper.addConceptToLocalSKOSCollection(modelID,"urn:uuid:"+conceptUUID);
                
		try {
			qexec.execute();
                        // TODO: Create JSON-LD?
                        return JerseyResponseManager.okUUID(conceptUUID);
			
		} catch (QueryExceptionHTTP ex) {
			logger.log(Level.WARNING, "Expect the unexpected!", ex);
			return JerseyResponseManager.unexpected();
		}
	}
        
        
  @POST
  @ApiOperation(value = "Update concept suggestion", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "New graph in application/ld+json", required = false) String body, 
          @ApiParam(value = "ConceptSuggestion ID", required = true) @QueryParam("conceptID") String conceptID,          
          @Context HttpServletRequest request) {
              
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        // TODO: !login.hasRightToEditModel(model) for concepts
        if(!login.isLoggedIn())
            return JerseyResponseManager.unauthorized();
                
        IRIFactory iriFactory = IRIFactory.iriImplementation(); 
        IRI conceptIRI;
        
        /* Check that URIs are valid */
        try {
            conceptIRI = iriFactory.construct(conceptID);
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }

        if(isNotEmpty(body)) {
            
        /* Put graph to database */ 
        StatusType status = JerseyJsonLDClient.putGraphToTheService(conceptID, body, services.getTempConceptReadWriteAddress());
        
           if (status.getFamily() != Response.Status.Family.SUCCESSFUL) {
               /* TODO: Create prov events from failed updates? */
               logger.log(Level.WARNING, "Unexpected: Not updated: "+conceptID);
               return JerseyResponseManager.unexpected(status.getStatusCode());
           } 
            
        } else {
             return JerseyResponseManager.invalidParameter();
        }

        ConceptMapper.updateConceptSuggestion(conceptID);
        
        return JerseyResponseManager.okEmptyContent();
        
  }
  
  // TODO: DO THIS FROM MODELCONCEPTS API: ConceptMapper.deleteConceptSuggestion(model,id);
  /*
  @DELETE
  @ApiOperation(value = "Delete concept suggestion", notes = "Deletes graph and references to it")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph"),
      @ApiResponse(code = 401, message = "Unauthorized")
  })
  public Response deleteConceptSuggestion(
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("modelID") String model,
          @ApiParam(value = "Concept ID", required = true) 
          @QueryParam("conceptID") String id,
          @Context HttpServletRequest request) {
      
      IRIFactory iriFactory = IRIFactory.iriImplementation();
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
      
       HttpSession session = request.getSession();

       if(session==null) return JerseyResponseManager.unauthorized();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return JerseyResponseManager.unauthorized();
       
       ConceptMapper.deleteConceptSuggestion(model,id);
       
       return return JerseyResponseManager.okEmptyContent();
  }
 */    
  
}
