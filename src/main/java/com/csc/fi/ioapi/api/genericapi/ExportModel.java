package com.csc.fi.ioapi.api.genericapi;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ContextWriter;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JsonSchemaWriter;
import com.csc.fi.ioapi.utils.XMLSchemaWriter;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.concurrent.TimeUnit;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("exportModel")
@Api(value = "/exportModel", description = "Export models")
public class ExportModel {

    @Context
    ServletContext context;
    EndpointServices services = new EndpointServices();

    private static final Logger logger = Logger.getLogger(ExportModel.class.getName());

    @GET
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
            @ApiParam(value = "Languages to export") @QueryParam("lang") String lang,
            @ApiParam(value = "Content-type", required = true, allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json,application/xml") @QueryParam("content-type") String ctype) {

        
        int wCount = 0;
        
        while(GraphManager.lock.isLocked()) {
            wCount+=1;
            try {
                logger.info("Sleeping ...");
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(ExportModel.class.getName()).log(Level.SEVERE, null, ex);
                return JerseyResponseManager.locked();
            }
            if(wCount>500) {
                logger.warning("GraphManager seems to be locked too long ...");
                return JerseyResponseManager.locked();
            }
        }
                
        /* Check that URIs are valid */
        if(IDManager.isInvalid(graph)) {
            return JerseyResponseManager.invalidIRI();
        }
            
            ctype = ctype.replace(" ", "+");
            
            if(ctype.equals("application/ld+json+context")) {
                String context = ContextWriter.newModelContext(graph);
                if(context!=null) {
                    return JerseyResponseManager.ok(context,raw?"text/plain;charset=utf-8":"application/json");
                } else {
                    return JerseyResponseManager.notFound();
                }
            } else if(ctype.equals("application/schema+json")) {
                String schema = null;
                if(lang!=null && !lang.equals("undefined") && !lang.equals("null")) {
                     logger.info("Exporting schema in "+lang);
                     schema = JsonSchemaWriter.newModelSchema(graph,lang);
                } else {
                    schema = JsonSchemaWriter.newMultilingualModelSchema(graph);
                }
                if(schema!=null) {
                    return JerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/schema+json");
                } else {
                    return JerseyResponseManager.langNotDefined();
                }
            } else if(ctype.equals("application/xml")) {
                
                String schema = XMLSchemaWriter.newModelSchema(graph, lang);
               
                if(schema!=null) {
                    return JerseyResponseManager.ok(schema,raw?"text/plain;charset=utf-8":"application/xml");
                } else {
                    return JerseyResponseManager.langNotDefined();
                }
            }
            
            /* IF ctype is none of the above try to export graph in RDF format */
            
            return JerseyJsonLDClient.getExportGraph(graph, raw, lang, ctype);

    }

}
