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
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
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
 * Root resource (exposed at "modelCreator" path)
 */
@Path("modelRequirementCreator")
@Api(value = "/modelRequirementCreator", description = "Construct new requirement")
public class ModeRequirementlCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ModeRequirementlCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new model", notes = "Create namespace object. Namespace must be valid URI and end with # or /.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newRequiredModel(
            @ApiParam(value = "Model namespace", required = true) @QueryParam("namespace") String namespace,
            @ApiParam(value = "Model prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Model label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

            ResponseBuilder rb;
            
            if(namespace==null || (!namespace.endsWith("#") && !namespace.endsWith("/"))) return Response.status(403).build();
            
            IRI namespaceIRI;
            
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    namespaceIRI = iri.construct(namespace);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).build();
            }

            Client client = Client.create();
             
            String queryString;
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            queryString = "CONSTRUCT  { ?g dcterms:title ?label . ?g dcap:preferredXMLNamespaceName ?namespace . ?g dcap:preferredXMLNamespacePrefix ?prefix . } WHERE { }";

            pss.setCommandText(queryString);
            pss.setIri("g", namespaceIRI);
            pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("namespace",namespace);
            pss.setLiteral("prefix", prefix);

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
 
}
