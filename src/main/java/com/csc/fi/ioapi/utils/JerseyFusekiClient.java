/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;
import com.hp.hpl.jena.query.QueryParseException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.uri.UriComponent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
/**
 *
 * @author malonen
 */
public class JerseyFusekiClient {
    
    static final private Logger logger = Logger.getLogger(JerseyFusekiClient.class.getName());
    
    public static Response getGraphResponseFromService(String id, String service) {
        try {
        Client client = Client.create();

        WebResource webResource = client.resource(service)
                                  .queryParam("graph", id);

        Builder builder = webResource.accept("application/ld+json");
        ClientResponse response = builder.get(ClientResponse.class);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           logger.log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+id);
           return Response.status(response.getStatus()).entity("{\"errorMessage\":\"Resource not found\"}").build();
        }

        ResponseBuilder rb = Response.status(response.getStatus()); 
        rb.entity(response.getEntityInputStream());

       return rb.build();
        } catch(ClientHandlerException ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{\"errorMessage\":\"Internal error\"}").build();
        }
    }
    
    public static ClientResponse putGraphToTheService(String graph, String body, String service) {
        Client client = Client.create();
        WebResource webResource = client.resource(service).queryParam("graph", graph);
        WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
        return builder.put(ClientResponse.class, body);
    }
    
    public static Response constructGraphFromService(String query, String service) {
        
        try {
            
            Client client = Client.create();

            WebResource webResource = client.resource(service)
                                      .queryParam("query", UriComponent.encode(query,UriComponent.Type.QUERY));

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);
            ResponseBuilder rb = Response.status(response.getStatus()); 
            rb.entity(response.getEntityInputStream());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               return Response.status(response.getStatus()).entity("{\"errorMessage\":\"Internal error\"}").build();
            }
            
            return rb.build();

      } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return Response.serverError().entity("{\"errorMessage\":\"Internal error\"}").build();
      }

    }
    
    public static Response deleteGraphFromService(String graph, String service) {
        
        try {

             Client client = Client.create();
             WebResource webResource = client.resource(service)
                                       .queryParam("graph", graph);

             WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
             ClientResponse response = builder.delete(ClientResponse.class);

             if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.WARNING, "Database connection error: "+graph+" was not deleted from "+service+"! Status "+response.getStatus());
                return Response.serverError().entity("{\"errorMessage\":\"Database connection error\"}").build();
             }
             
             return Response.status(204).build();

       } catch(Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.serverError().entity("{\"errorMessage\":\"Internal error\"}").build();
       }
        
        
    }
    
}
