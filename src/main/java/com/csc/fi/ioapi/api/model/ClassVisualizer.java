package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
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
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.ByteArrayInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
 
/**
 * Root resource (exposed at "classCreator" path)
 */
@Path("classVisualizer")
@Api(value = "/classVisualizer", description = "Construct new Class template")
public class ClassVisualizer {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ClassVisualizer.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create 1 level from class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response visClass(
            @ApiParam(value = "Class ID", required = true) @QueryParam("classID") String classID,
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID
    ) {

              ResponseBuilder rb;

              IRI classIRI, modelIRI;


              try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    classIRI = iri.construct(classID);
                    modelIRI = iri.construct(modelID);
              } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).build();
              }

              DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
              Model model = accessor.getModel(modelID);
              
              
                Client client = Client.create();
             
                String queryString;
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
               // pss.setNsPrefixes(LDHelper.PREFIX_MAP);
                pss.setNsPrefixes(model.getNsPrefixMap());
                queryString = "CONSTRUCT  { "
                        + "?classIRI a sh:ShapeClass . "
                        + "?classIRI rdfs:label ?classLabel . "
                        + "?classIRI sh:property ?property . "
                        + "?property sh:predicate ?predicate . "
                        + "?property rdfs:label ?propertyLabel . "
                        + "?property ?range ?classRef . "
                        + "?classRef a sh:ShapeClass . "
                        + "?classRef rdfs:label ?classRefLabel . "
                        + "?classRef sh:property ?propertyRef . "
                        + "?propertyRef sh:predicate ?predicateRef . "
                        + "?propertyRef rdfs:label ?propertyRefLabel . "
                        + "?propertyRef ?refRange ?propertyRefRange . "
                        + "} WHERE { "
                        + "GRAPH ?classIRI { "
                        + "?classIRI a sh:ShapeClass . "
                        + "?classIRI rdfs:label ?classLabel . "
                        + "?classIRI sh:property ?property . "
                        + "?property rdfs:label ?propertyLabel . "
                        + "?property sh:predicate ?predicate . "
                        + "VALUES ?range { sh:valueClass sh:datatype } "
                        + "?property ?range ?classRef . "
                        + "}"
                        + "OPTIONAL { "
                        + "GRAPH ?classRef { ?classRef a sh:ShapeClass . "
                        + "?classRef rdfs:label ?classRefLabel . "
                        + "?classRef sh:property ?propertyRef . "
                        + "?propertyRef sh:predicate ?predicateRef . "
                        + "?propertyRef rdfs:label ?propertyRefLabel . "
                        + "VALUES ?refRange { sh:valueClass sh:datatype } "
                        + "?propertyRef ?refRange ?propertyRefRange . "
                        + "}}"
                        + "}";
               
                pss.setCommandText(queryString);
                pss.setIri("classIRI", classIRI);

                logger.info(pss.toString());
                WebResource webResource = client.resource(services.getCoreSparqlAddress())
                         .queryParam("query", UriComponent.encode(pss.toString(),UriComponent.Type.QUERY));

                Builder builder = webResource.accept("application/ld+json");
                ClientResponse response = builder.get(ClientResponse.class);

               if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    logger.log(Level.WARNING, response.toString());
                    return Response.status(response.getStatus()).entity("{}").build();
                } 
            
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
            
            
                try {
                        return rb.build();
                } catch (QueryExceptionHTTP ex) {
                        logger.log(Level.WARNING, "Expect the unexpected!", ex);
                        return Response.status(400).build();
                }
    }   
 
}
