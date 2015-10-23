package com.csc.fi.ioapi.api.model;

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
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
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
 * Root resource (exposed at "myresource" path)
 */
@Path("classCreator")
@Api(value = "/classCreator", description = "Operations about property")
public class ClassCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ClassCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Concept ID", required = true) @QueryParam("conceptID") String conceptID,
            @Context HttpServletRequest request) {

          ResponseBuilder rb;

            IRI conceptIRI,modelIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    conceptIRI = iri.construct(conceptID);
                    modelIRI = iri.construct(modelID);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "CLASS OR PROPERTY ID is invalid IRI!");
                    return Response.status(403).build();
            }

        /*
        HttpSession session = request.getSession();

        if(session==null) return Response.status(401).build();

        LoginSession login = new LoginSession(session);

        if(!login.isLoggedIn() || !login.hasRightToEdit(modelID))
            return Response.status(401).build();
        */
        
            updateConceptstoCoreService(conceptID);
        
             Client client = Client.create();
             
                String queryString;
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.setNsPrefixes(LDHelper.PREFIX_MAP);
                queryString = "CONSTRUCT  { ?classIRI a sh:ShapeClass . ?classIRI rdfs:isDefinedBy ?model . ?classIRI rdfs:label ?label . ?classIRI rdfs:comment ?comment . ?classIRI dcterms:subject ?concept . ?concept skos:prefLabel ?label . ?concept rdfs:comment ?comment . } WHERE {{ ?concept a skos:Concept . ?concept skos:prefLabel ?label . OPTIONAL {?concept rdfs:comment ?comment . }} UNION { ?concept skos:prefLabel ?label . FILTER(lang(?label)='fi') BIND(IRI(CONCAT(?namespace,ENCODE_FOR_URI(REPLACE(UCASE(STR(?label)),' ','')))) as ?classIRI) } }";
                pss.setCommandText(queryString);
                pss.setIri("concept", conceptIRI);
                pss.setIri("model", modelIRI);
                pss.setLiteral("namespace",modelID+"#");

                logger.info(pss.toString());
                WebResource webResource = client.resource(services.getTempConceptReadSparqlAddress())
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
        
     private void updateConceptstoCoreService(String uri) {
            
         
         // TODO: ADD TEST FOR SUGGESTIONS AND TEMP CONCEPTS 
         
            Client client = Client.create();
            
            WebResource webResource = client.resource(services.getConceptAPI())
                                      .queryParam("uri", UriComponent.encode(uri,UriComponent.Type.QUERY))
                                      .queryParam("format","application/json");

            Builder builder = webResource.accept("application/json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.warning("Could not find the concept");
            }

            Model model = ModelFactory.createDefaultModel(); 
            
            RDFDataMgr.read(model, response.getEntityInputStream(), RDFLanguages.JSONLD);
            
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getTempConceptReadWriteAddress());
            accessor.add(uri, model);
  
    }       
 
}
