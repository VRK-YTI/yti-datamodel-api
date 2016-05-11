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
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.OPHCodeServer;
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
 
/**
 * Root resource (exposed at "valueSchemeCreator" path)
 */
@Path("coreListCreator")
@Api(value = "/codeListCreator", description = "Create reusable code list that is not resolved")
public class CodeListCreator {

    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(CodeListCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new code list", notes = "Creates new code list")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New list is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newValueScheme(
            @ApiParam(value = "Codelist uri", required = true) @QueryParam("uri") String uri,
            @ApiParam(value = "Codelist name", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Codelist description", required = true) @QueryParam("description") String comment,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

                    
            IRIFactory iri = IRIFactory.iriImplementation();
            IRI codeListIRI = null;
        
            if(uri!=null && !uri.equals("undefined")) {
                try{
                    codeListIRI = iri.construct(uri.toLowerCase());
                } catch(IRIException e) {
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
                }
            } else
                return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();

           
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            queryString = "CONSTRUCT  { "
                    + "?iri a dcam:VocabularyEncodingScheme . "
                    + "?iri dcterms:title ?label . "
                    + "?iri dcterms:description ?description . "
                    + "?iri dcterms:identifier ?uuid . "
                    + "?iri dcterms:isPartOf iow:OtherCodeGroup . "
                    + "iow:OtherCodeGroup dcterms:title 'Muut luokitukset'@fi . "
                    + "iow:OtherCodeGroup dcterms:title 'Reference data'@en . "
                    + "} WHERE { "
                    + "BIND(UUID() as ?uuid) "
                    + " }";

            pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("description", ResourceFactory.createLangLiteral(comment, lang));
            pss.setIri("iri",codeListIRI);
            
            pss.setCommandText(queryString);

            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());

        }
}
