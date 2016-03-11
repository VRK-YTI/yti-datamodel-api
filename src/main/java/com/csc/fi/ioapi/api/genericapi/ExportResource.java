package com.csc.fi.ioapi.api.genericapi;

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
import com.csc.fi.ioapi.utils.ContextWriter;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.JsonSchemaWriter;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("exportResource")
@Api(value = "/exportResource", description = "Export Classes, Predicates, Shapes etc.")
public class ExportResource {

    @Context
    ServletContext context;
    EndpointServices services = new EndpointServices();

    private static final Logger logger = Logger.getLogger(ExportResource.class.getName());

    @GET
    @Produces("text/plain")
    @ApiOperation(value = "Get model from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 403, message = "Invalid model id"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @ApiParam(value = "Requested resource", defaultValue = "default") @QueryParam("graph") String graph,
            @ApiParam(value = "Raw / PlainText boolean", defaultValue = "false") @QueryParam("raw") boolean raw,
            @ApiParam(value = "Content-type", required = true, allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json") @QueryParam("content-type") String ctype) {

        
         IRI resourceIRI;
         
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    resourceIRI = iri.construct(graph);
            } catch (IRIException e) {
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }
            
            if(ctype.equals("application/ld+json+context")) {
                String context = ContextWriter.newClassContext(graph);
                if(context!=null) {
                    return Response.ok().entity(context).type(raw?"text/plain;charset=utf-8":"application/json").build();
                } else {
                    return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
                }
            } else if(ctype.equals("application/schema+json")) {
                String schema = JsonSchemaWriter.newClassSchema(graph);
                if(schema!=null) {
                    return Response.ok().entity(schema).type(raw?"text/plain;charset=utf-8":"application/schema+json").build();
                } else {
                    return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
                }
            }
            
        try {
            ContentType contentType = ContentType.create(ctype);
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            
            if(rdfLang==null) return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
                        
            return  JerseyFusekiClient.getGraphResponseFromService(graph, services.getCoreReadAddress(), contentType, true);
        } catch (UniformInterfaceException | ClientHandlerException ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.serverError().entity("{}").build();
        }

    }

}
