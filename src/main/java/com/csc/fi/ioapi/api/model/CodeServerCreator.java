/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
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
@Path("coreServerCreator")
@Api(value = "/codeServerCreator", description = "Create new reference to code server")
public class CodeServerCreator {

    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(CodeServerCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new value scheme", notes = "Creates value scheme object")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newValueScheme(
            @ApiParam(value = "Code server api uri", required = true) @QueryParam("uri") String uri,
            @ApiParam(value = "Code server name", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Code server description", required = true) @QueryParam("description") String comment,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @ApiParam(value = "Update codelists", defaultValue = "false") @QueryParam("update") boolean force) {

                    
            IRIFactory iri = IRIFactory.iriImplementation();
            IRI codeServerIRI = null;
        
            if(uri!=null && !uri.equals("undefined")) {
                
                OPHCodeServer codeServer = new OPHCodeServer(uri, true);
                
                if(codeServer==null || !codeServer.status) {
                    return JerseyResponseManager.invalidParameter();
                }
                try{
                    codeServerIRI = iri.construct(uri);
                } catch(IRIException e) {
                    return JerseyResponseManager.invalidIRI();
                }

            } else
                return JerseyResponseManager.invalidParameter();

           
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            queryString = "CONSTRUCT  { "
                    + "?iri a ?type . "
                    + "?iri dcterms:title ?label . "
                    + "?iri dcterms:description ?description . "
                    + "?iri dcterms:identifier ?uuid . "
                    + "} WHERE { "
                    + "BIND(UUID() as ?uuid) "
                    + " }";

            pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("description", ResourceFactory.createLangLiteral(comment, lang));
            pss.setIri("iri",uri);
            
            pss.setCommandText(queryString);

            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getTempConceptReadSparqlAddress());

        }
}
