/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
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
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    
                    if(conceptID!=null && !conceptID.equals("undefined")) conceptIRI = iri.construct(conceptID);
                    if(userID!=null && !userID.equals("undefined")) userIRI = iri.construct(userID);
                    if(schemeID!=null && !schemeID.equals("undefined")) schemeIRI = iri.construct(schemeID);
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
                
          WebResource webResource = client.resource(services.getTempConceptReadSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

          WebResource.Builder builder = webResource.accept("application/ld+json");

          ClientResponse response = builder.get(ClientResponse.class);
          rb = Response.status(response.getStatus()); 
          rb.entity(response.getEntityInputStream());
            
          return rb.build();
           
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

                if(session==null) return Response.status(401).build();

                LoginSession login = new LoginSession(session);

                if(!login.isLoggedIn()) return Response.status(401).build();
            
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
			return Response.status(401).entity(ErrorMessage.INVALIDIRI).build();
		} catch(NullPointerException e) {
                    	logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
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
			return Response.status(200).entity("{\"@id\":\"urn:uuid:"+conceptUUID+"\"}").build();
		} catch (QueryExceptionHTTP ex) {
			logger.log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
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
        
        if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
        
        LoginSession login = new LoginSession(session);
        
        // TODO: !login.hasRightToEditModel(model) for concepts
        if(!login.isLoggedIn())
            return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
                
        IRIFactory iriFactory = IRIFactory.iriImplementation(); 
        IRI conceptIRI;
        
        /* Check that URIs are valid */
        try {
            conceptIRI = iriFactory.construct(conceptID);
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }

        if(isNotEmpty(body)) {
            
        /* Put graph to database */ 
        ClientResponse response = JerseyFusekiClient.putGraphToTheService(conceptID, body, services.getTempConceptReadWriteAddress());
           
           if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               /* TODO: Create prov events from failed updates? */
               logger.log(Level.WARNING, "Unexpected: Not updated: "+conceptID);
               return Response.status(response.getStatus()).entity(ErrorMessage.UNEXPECTED).build();
           } 
            
        } else {
             return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
        }
        
        return Response.status(204).entity("{}").build();
        
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
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
      
       HttpSession session = request.getSession();

       if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
       
       ConceptMapper.deleteConceptSuggestion(model,id);
       
       return Response.status(204).entity("{}").build();
  }
 */    
  
}
