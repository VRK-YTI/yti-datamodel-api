/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import com.csc.fi.ioapi.config.ApplicationProperties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.NamespaceResolver;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.util.SplitIRI;
 
/**
 * Root resource (exposed at "valueSchemeCreator" path)
 */
@Path("valueSchemeCreator")
@Api(value = "/valueSchemeCreator", description = "Construct new requirement")
public class ValueSchemeCreator {

    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ValueSchemeCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new value scheme", notes = "Creates value scheme object")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newValueScheme(
            @ApiParam(value = "Value scheme IRI", required = true) @QueryParam("namespace") String valueScheme,
            @ApiParam(value = "Value scheme name", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Value scheme comment", required = true) @QueryParam("comment") String comment,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

            IRI valueSchemeIRI;

            try {
                    IRIFactory iri = IRIFactory.iriImplementation();
                    valueSchemeIRI = iri.construct(valueScheme);
            } catch (IRIException e) {
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }

            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            queryString = "CONSTRUCT  { "
                    + "?valueScheme a dcam:VocabularyEncodingScheme . "
                    + "?valueScheme rdfs:label ?label . "
                    + "?valueScheme rdfs:comment ?description . "
                    + "} WHERE { }";

            pss.setCommandText(queryString);

            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());

        }
}
