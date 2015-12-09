/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.Map;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;

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
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            Map<String,String> namespaces = GraphManager.getNamespaceMap();
            namespaces.putAll(LDHelper.PREFIX_MAP);
            pss.setNsPrefixes(namespaces);
            
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
