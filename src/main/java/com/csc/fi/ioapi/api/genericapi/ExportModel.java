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
import javax.ws.rs.core.Response.ResponseBuilder;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ContextWriter;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.JsonSchemaWriter;
import com.csc.fi.ioapi.utils.LDHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

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
            @ApiParam(value = "Content-type", required = true, allowableValues = "application/ld+json,text/turtle,application/rdf+xml,application/ld+json+context,application/schema+json") @QueryParam("content-type") String ctype) {

         IRI modelIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    modelIRI = iri.construct(graph);
            } catch (IRIException e) {
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }
            
            if(ctype.equals("application/ld+json+context")) {
                String context = ContextWriter.newModelContext(graph);
                if(context!=null) {
                    return Response.ok().entity(context).type(raw?"text/plain;charset=utf-8":"application/json").build();
                } else {
                    return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
                }
            } else if(ctype.equals("application/schema+json")) {
                String schema = JsonSchemaWriter.newModelSchema(graph,lang);
                if(schema!=null) {
                    return Response.ok().entity(schema).type(raw?"text/plain;charset=utf-8":"application/schema+json").build();
                } else {
                    return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
                }
            }
            
        try {
            
            ctype = ctype.replace(" ", "+");
            ContentType contentType = ContentType.create(ctype);
            
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            
            
            if(rdfLang==null) {
                logger.info("Unknown RDF type: "+ctype);
                return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
            }

            
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
            Model model = accessor.getModel(graph);

            OutputStream out = new ByteArrayOutputStream();
            
            ClientResponse response = JerseyFusekiClient.getGraphClientResponseFromService(graph+"#ExportGraph", services.getCoreReadAddress(),ctype);
          
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                return Response.status(400).entity(ErrorMessage.UNEXPECTED).build();
            }

            ResponseBuilder rb;
            RDFDataMgr.write(out, model, rdfLang);

            if (rdfLang.equals(Lang.JSONLD)) {

                Map<String, Object> jsonModel = null;
                try {
                    jsonModel = (Map<String, Object>) JsonUtils.fromString(out.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ExportModel.class.getName()).log(Level.SEVERE, null, ex);
                    return Response.status(400).entity(ErrorMessage.UNEXPECTED).build();
                }

                Map<String, Object> frame = new HashMap<String, Object>();
                //Map<String,Object> frame = (HashMap<String,Object>) LDHelper.getExportContext();

                Map<String, Object> context = (Map<String, Object>) jsonModel.get("@context");

                context.putAll(LDHelper.CONTEXT_MAP);

                frame.put("@context", context);
                frame.put("@type", "owl:Ontology");

                Object data;

                try {                 
                    data = JsonUtils.fromInputStream(response.getEntityInputStream());
                    
                    rb = Response.status(response.getStatus());

                    try {
                        JsonLdOptions options = new JsonLdOptions();
                        Object framed = JsonLdProcessor.frame(data, frame, options);
                        
                        ObjectMapper mapper = new ObjectMapper();
 
                        rb.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(framed));
                        
                    } catch (NullPointerException ex) {
                        logger.log(Level.WARNING, null, "DEFAULT GRAPH IS NULL!");
                        return rb.entity(JsonUtils.toString(data)).build();
                    } catch (JsonLdError ex) {
                        logger.log(Level.SEVERE, null, ex);
                        return Response.serverError().entity("{}").build();
                    }

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return Response.serverError().entity("{}").build();
                }

            } else {
                 rb = Response.status(response.getStatus());
                 rb.entity(response.getEntityInputStream());
            }

            if(!raw) {
                rb.type(contentType.getContentType());
            } else {
                rb.type("text/plain");
            }
            
            return rb.build();

        } catch (UniformInterfaceException | ClientHandlerException ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.serverError().entity("{}").build();
        }

    }

}
