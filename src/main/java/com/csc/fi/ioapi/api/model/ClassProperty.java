package com.csc.fi.ioapi.api.model;

import com.csc.fi.ioapi.api.usermanagement.User;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malonen
 */
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("classProperty")
@Api(value = "/classProperty", description = "Operations about property")
public class ClassProperty {

    @Context ServletContext context;
    private EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ClassProperty.class.getName());
    
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get property from model", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
	@ApiParam(value = "Predicate ID", required = true) @QueryParam("predicateID") String predicateID) {

      
        IRI predicateIRI;
		try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    predicateIRI = iri.construct(predicateID);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return Response.status(403).build();
		}
                
          ResponseBuilder rb;
          
          Client client = Client.create();
          String queryString;
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
          queryString = "CONSTRUCT { ?uuid sh:predicate ?predicate . ?uuid rdfs:label ?label . ?uuid rdfs:comment ?comment . ?uuid sh:valueClass ?valueClass . ?uuid sh:datatype ?datatype . } WHERE { BIND(UUID() as ?uuid) OPTIONAL { GRAPH ?predicate { ?predicate rdfs:label ?label .  OPTIONAL{ ?predicate rdfs:comment ?comment . } OPTIONAL{ ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . } OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . }}} }";
  	  
          pss.setCommandText(queryString);
          pss.setIri("predicate", predicateIRI);

         
          WebResource webResource = client.resource(services.getCoreSparqlAddress())
                                      .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

          WebResource.Builder builder = webResource.accept("application/ld+json");

          ClientResponse response = builder.get(ClientResponse.class);
          rb = Response.status(response.getStatus()); 
          rb.entity(response.getEntityInputStream());
            
          return rb.build();
           
  }
 
  	@PUT
	@ApiOperation(value = "Create new class property", notes = "Create new class property")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "New property is created"),
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 403, message = "Invalid IRI in parameter"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response newClassProperty(
                @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
                @ApiParam(value = "Class ID", required = true) @QueryParam("classID") String classID,
		@ApiParam(value = "Predicate ID", required = true) @QueryParam("predicateID") String predicateID) {

		IRI classIRI,predicateIRI,modelIRI;
		try {
			IRIFactory iri = IRIFactory.semanticWebImplementation();

			classIRI = iri.construct(classID);
                        predicateIRI = iri.construct(predicateID);
                        modelIRI = iri.construct(modelID);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return Response.status(403).build();
		}

		String queryString;
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setNsPrefixes(LDHelper.PREFIX_MAP);
		queryString = "INSERT { GRAPH ?class { ?class a sh:ShapeClass . ?class sh:property ?uuid . ?uuid sh:predicate ?predicate . ?uuid rdfs:label ?label . ?uuid rdfs:comment ?comment . ?uuid sh:valueClass ?valueClass . ?uuid sh:datatype ?datatype . }} WHERE { GRAPH ?class { ?class a sh:ShapeClass . ?class rdfs:isDefinedBy ?model . BIND(UUID() as ?uuid) } OPTIONAL { GRAPH ?predicate { ?predicate rdfs:label ?label .  OPTIONAL{ ?predicate rdfs:comment ?comment . } OPTIONAL{ ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . } OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . }}} }";
		pss.setCommandText(queryString);
		pss.setIri("class", classIRI);
                pss.setIri("predicate", predicateIRI);
                pss.setIri("model", modelIRI);

                logger.log(Level.INFO,pss.toString());
		UpdateRequest query = pss.asUpdate();
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(query, services.getCoreSparqlUpdateAddress() );

		try {
			qexec.execute();
			return Response.status(200).build();
		} catch (QueryExceptionHTTP ex) {
			logger.log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
		}
	}
        
        
        @DELETE
	@ApiOperation(value = "Create new class property", notes = "Create new class property")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "New property is created"),
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 403, message = "Invalid IRI in parameter"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response removeClassProperty(
                @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
                @ApiParam(value = "Class ID", required = true) @QueryParam("classID") String classID,
		@ApiParam(value = "Property UUID", required = true) @QueryParam("propertyUUID") String propertyUUID) {

		IRI classIRI,propertyIRI,modelIRI;
		try {
			IRIFactory iri = IRIFactory.semanticWebImplementation();
			classIRI = iri.construct(classID);
                        propertyIRI = iri.construct(propertyUUID);
                        modelIRI = iri.construct(modelID);
		} catch (IRIException e) {
			logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
			return Response.status(403).build();
		}

		String queryString;
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setNsPrefixes(LDHelper.PREFIX_MAP);
		queryString = "DELETE { GRAPH ?class { ?class sh:property ?property . ?property ?p ?o . } } WHERE { GRAPH ?class { ?class a sh:ShapeClass . ?class rdfs:isDefinedBy ?model . ?class sh:property ?property . }  }";
		pss.setCommandText(queryString);
		pss.setIri("class", classIRI);
                pss.setIri("property", propertyIRI);
                pss.setIri("model", modelIRI);

                logger.log(Level.INFO,pss.toString());
		UpdateRequest query = pss.asUpdate();
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(query, services.getCoreSparqlUpdateAddress() );

		try {
			qexec.execute();
			return Response.status(200).build();
		} catch (QueryExceptionHTTP ex) {
			logger.log(Level.WARNING, "Expect the unexpected!", ex);
			return Response.status(400).build();
		}
	}
  
}
