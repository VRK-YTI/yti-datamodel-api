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
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malonen
 */

/**
 * Root resource (exposed at "usage" path)
 */
@Path("usage")
@Api(value = "/usage", description = "Returns all known references to the given resource")
public class Usage {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Usage.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Resource ID", required = true) @QueryParam("id") String id) {

            IRI resourceIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    resourceIRI = iri.construct(id);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).entity("{\"errorMessage\":\"Invalid id\"}").build();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            String queryString = "CONSTRUCT  { "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . "
                    + "?resource dcterms:isReferencedBy ?usage . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "?usage rdfs:isDefinedBy ?usageModel . "
                    + "} WHERE { "
                    + "GRAPH ?resource { "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . }"
                    + "GRAPH ?usage { "
                    + "?subject ?property ?resource . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "OPTIONAL {?usage rdfs:isDefinedBy ?usageModel . }}"
                    + "FILTER(?usage!=?resourceModel)"
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("resource", resourceIRI);

            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
    }   
 
}
