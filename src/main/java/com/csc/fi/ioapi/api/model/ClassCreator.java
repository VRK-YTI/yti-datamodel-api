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
 * Root resource (exposed at "classCreator" path)
 */
@Path("classCreator")
@Api(value = "/classCreator", description = "Construct new Class template")
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
            @ApiParam(value = "Class label", required = true) @QueryParam("classLabel") String classLabel,
            @ApiParam(value = "Concept ID", required = true) @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

            IRI conceptIRI,modelIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    conceptIRI = iri.construct(conceptID);
                    modelIRI = iri.construct(modelID);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).entity("{\"errorMessage\":\"Invalid id\"}").build();
            }

            ConceptMapper.updateConceptFromConceptService(conceptID);

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            String queryString = "CONSTRUCT  { "
                    + "?classIRI owl:versionInfo ?draft . "
                    + "?classIRI dcterms:modified ?modified . "
                    + "?classIRI dcterms:created ?creation . "
                    + "?classIRI a sh:ShapeClass . "
                    + "?classIRI rdfs:isDefinedBy ?model . "
                    + "?classIRI rdfs:label ?classLabel . "
                    + "?classIRI rdfs:comment ?comment . "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:prefLabel ?label . "
                    + "?concept rdfs:comment ?comment . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "?concept a skos:Concept . "
                    + "VALUES ?someLabel { rdfs:label skos:prefLabel} "
                    + "?concept ?someLabel ?label . "
                    + "OPTIONAL {?concept rdfs:comment ?comment . } "
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("concept", conceptIRI);
            pss.setIri("model", modelIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("classLabel", ResourceFactory.createLangLiteral(classLabel, lang));
            pss.setIri("classIRI",modelID+"#"+LDHelper.resourceName(classLabel));

            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());
    }   
 
}
