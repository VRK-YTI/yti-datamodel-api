package com.csc.fi.ioapi.api.genericapi;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ContextWriter;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JsonSchemaWriter;
import com.csc.fi.ioapi.utils.XMLSchemaWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.HeaderParam;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("exportResource")
@Api(tags = {"Resource"}, description = "Export Classes, Predicates, Shapes etc.")
public class ExportResource {

    @Context
    ServletContext context;
    EndpointServices services = new EndpointServices();

    private static final Logger logger = Logger.getLogger(ExportResource.class.getName());

    @GET
    @ApiOperation(value = "Get model from service", notes = "More notes about this method")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid model supplied"),
        @ApiResponse(code = 403, message = "Invalid model id"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @HeaderParam("Accept") String accept,
            @ApiParam(value = "Requested resource", defaultValue = "default") @QueryParam("graph") String graph,
            @ApiParam(value = "Raw / PlainText boolean", defaultValue = "false") @QueryParam("raw") boolean raw,
            @ApiParam(value = "Languages to export") @QueryParam("lang") String lang,
            @ApiParam(value = "Service to export") @QueryParam("service") String serviceString,
            @ApiParam(value = "Content-type", allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json,application/xml") @QueryParam("content-type") String ctype) {

        if(ctype==null || ctype.equals("undefined")) ctype = accept;
        
        String service = services.getCoreReadAddress();
        
        if(serviceString!=null && !serviceString.equals("undefined")) {
            if(serviceString.equals("concept")) {
                service = services.getTempConceptReadWriteAddress();
            }
        }
        
        /* Check that URIs are valid */
        if(IDManager.isInvalid(graph)) {
            return JerseyResponseManager.invalidIRI();
        }
            
            if(ctype.equals("application/ld+json+context")) {
                String context = ContextWriter.newResourceContext(graph);
                if(context!=null) {
                    return JerseyResponseManager.ok(context,raw?"text/plain;charset=utf-8":"application/json");
                } else {
                    return JerseyResponseManager.notFound();
                }
            } else if(ctype.equals("application/schema+json")) {
                String schema = JsonSchemaWriter.newResourceSchema(graph,lang);
                if(schema!=null) {
                    return JerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/schema+json");
                } else {
                    return JerseyResponseManager.notFound();
                }
            } else if(ctype.equals("application/xml")) {
                
                String schema = XMLSchemaWriter.newClassSchema(graph,lang);
               
                if(schema!=null) {
                    return JerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/schema+json");
                } else {
                    return JerseyResponseManager.notFound();
                }
            }
            
            
        try {
            ContentType contentType = ContentType.create(ctype);
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            
            if(rdfLang==null) {
                rdfLang = Lang.TURTLE;
                //return JerseyResponseManager.notFound();
            }
                        
            return  JerseyJsonLDClient.getGraphResponseFromService(graph, service, contentType, raw);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return JerseyResponseManager.serverError();
        }

    }

}
