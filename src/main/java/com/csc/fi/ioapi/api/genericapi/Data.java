package com.csc.fi.ioapi.api.genericapi;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malonen
 */
import com.csc.fi.ioapi.api.profile.Profile;
import com.csc.fi.ioapi.utils.LDHelper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import static com.github.jsonldjava.core.JsonLdProcessor.compact;
import com.github.jsonldjava.utils.JsonUtils;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import com.hp.hpl.jena.query.DatasetAccessor ;
import com.hp.hpl.jena.query.DatasetAccessorFactory ;
import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource.Builder;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import static javax.ws.rs.HttpMethod.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("data")
@Api(value = "/data", description = "Operations about data")
public class Data {

    @Context ServletContext context;

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String sayHtmlHello() {
    return "<html> " + "<title>" + "Are you lost?" + "</title>"
        + "<body><h1>" + "Hello stranger!" + "</h1><p>Looking for <a href=\"../swagger-ui\">API?</a></p></body>" + "</html> ";
  }

  
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get model from service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(@ApiParam(value = "Requested service", required=true) @QueryParam("service") String service, @ApiParam(value = "Requested resource", defaultValue="default") @QueryParam("graph") String graph) {      
      try {
          
            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("graph", graph);

            Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatus() != 200) {
               Logger.getLogger(Data.class.getName()).log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+graph);
               return Response.status(response.getStatus()).entity("{}").build();
            }
            
            ResponseBuilder rb;
            rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
            
           return rb.build();
      
      } catch(UniformInterfaceException | ClientHandlerException ex) {
          Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{}").build();
      } 

  }
  
  
  @POST
  @ApiOperation(value = "Get framed model from service", notes = "Frame is required in the POST Body")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
  @Consumes("application/ld+json") // Content-Type
  @Produces("application/ld+json")
  public Response frameJson(@ApiParam(value = "Requested service", required=true) @QueryParam("service") String service, @ApiParam(value = "Context in application/ld+json", required = true) String inputContext, @ApiParam(value = "Requested graph", required = true) @QueryParam("graph") String graph) {

    try {
        
        Client client = Client.create();

        WebResource webResource = client.resource(service)
                                  .queryParam("graph", graph);

        Builder builder = webResource.accept("application/ld+json");

        ClientResponse response = builder.get(ClientResponse.class);

        if (response.getStatus() != 200) {
            Logger.getLogger(Data.class.getName()).log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+graph);
            return Response.status(response.getStatus()).build();
        }

        try {
            // Object data = JsonUtils.fromInputStream(response.getEntityInputStream());
            Object context = JsonUtils.fromString(inputContext);

            // BUG FIX == https://issues.apache.org/jira/browse/JENA-794
            JsonObject json = JSON.parse(response.getEntityInputStream());
            JsonValue jsonGraph = json.get("@graph");
            JsonValue jsonContext = json.get("@context");
            json.remove("@id");

            Object data = JsonUtils.fromString(json.toString());

            JsonLdOptions options = new JsonLdOptions();
            options.format = "application/ld+json";
            options.useNamespaces = true ;
            options.setUseNativeTypes(true);
            options.setCompactArrays(true);

           // Object compact = JsonLdProcessor.compact(data, context, options); 
            Object framed = JsonLdProcessor.frame(data, context, options); 

            ResponseBuilder rb = Response.status(response.getStatus()); 

            rb.entity(JsonUtils.toString(framed));
            return rb.build();

        } catch (IOException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Error in reading JSON-LD", ex);
            return Response.status(400).build();
        } catch (JsonLdError ex) {
            Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Error in reading JSON-LD", ex);
            return Response.status(400).build();
        }
    
    } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Data.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
    }
  }
}