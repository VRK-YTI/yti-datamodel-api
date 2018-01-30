/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyClient;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
 
/**
 * Root resource (exposed at "valueSchemeCreator" path)
 */
@Path("codeListCreator")
@Api(tags = {"Codes"}, description = "Create reusable code list that is not resolved")
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

                    
            IRI codeListIRI = null;
        
            if(uri!=null && !uri.equals("undefined")) {
                try{
                    codeListIRI = IDManager.constructIRI(uri.toLowerCase());
                } catch(IRIException e) {
                    return JerseyResponseManager.invalidIRI();
                }
            } else {
                return JerseyResponseManager.invalidParameter();
            }
                
           
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

            return JerseyClient.constructNonEmptyGraphFromService(pss.toString(), services.getSchemesSparqlAddress());

        }
}
