/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
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
import com.csc.fi.ioapi.utils.ErrorMessage;
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

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("predicateCreator")
@Api(value = "/predicateCreator", description = "Creates new RDF properties based on SKOS concepts")
public class PredicateCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(PredicateCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new predicate", notes = "Create new predicate")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New predicate is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newPredicate(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Predicate label", required = true) @QueryParam("predicateLabel") String predicateLabel,
            @ApiParam(value = "Concept ID", required = true) @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Predicate type", required = true, allowableValues="owl:DatatypeProperty,owl:ObjectProperty") @QueryParam("type") String type,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

        IRI conceptIRI,modelIRI,typeIRI;
        try {
                String typeURI = type.replace("owl:", "http://www.w3.org/2002/07/owl#");
                IRIFactory iri = IRIFactory.semanticWebImplementation();
                conceptIRI = iri.construct(conceptID);
                modelIRI = iri.construct(modelID);
                typeIRI = iri.construct(typeURI);
        } catch (IRIException e) {
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }

        ConceptMapper.updateConceptFromConceptService(conceptID);

        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        queryString = "CONSTRUCT  { ?predicateIRI owl:versionInfo ?draft . ?predicateIRI dcterms:created ?creation . ?predicateIRI dcterms:modified ?modified . ?predicateIRI a ?type .  ?predicateIRI rdfs:isDefinedBy ?model . ?predicateIRI rdfs:label ?predicateLabel . ?predicateIRI rdfs:comment ?comment . ?predicateIRI dcterms:subject ?concept . ?concept skos:prefLabel ?label . ?concept rdfs:comment ?comment . } WHERE { BIND(now() as ?creation) BIND(now() as ?modified) ?concept a skos:Concept . VALUES ?someLabel {rdfs:label skos:prefLabel} ?concept ?someLabel ?label . OPTIONAL {?concept rdfs:comment ?comment . } }";

        pss.setCommandText(queryString);
        pss.setIri("concept", conceptIRI);
        pss.setIri("model", modelIRI);
        pss.setIri("type", typeIRI);
        pss.setLiteral("draft", "Unstable");
        pss.setLiteral("predicateLabel", ResourceFactory.createLangLiteral(predicateLabel, lang));
        pss.setIri("predicateIRI",modelID+"#"+LDHelper.propertyName(predicateLabel));

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());

    }   
 
}
