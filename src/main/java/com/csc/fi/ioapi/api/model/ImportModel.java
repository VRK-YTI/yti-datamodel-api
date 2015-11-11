package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
 
/**
 * Root resource (exposed at "importModel" path)
 */
@Path("importModel")
@Api(value = "/importModel", description = "Import existing model")
public class ImportModel {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ImportModel.class.getName());
  
  /* TODO: Remove? */
  /*
  @POST
  @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) String body, 
          @ApiParam(value = "Graph to overwrite", required = true) @QueryParam("graph") String graph) {
      
       if(graph.equals("default") || graph.equals("undefined")) {
            return Response.status(403).build();
       } 
       try {

           ServiceDescriptionManager.updateGraphDescription(graph);
          
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreReadWriteAddress()).queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            logger.log(Level.FINE, graph+" updated sucessfully!");
            
            Model model = ModelFactory.createDefaultModel();
            
            RDFDataMgr.read(model, response.getEntityInputStream(),RDFLanguages.JSONLD);
            
            logger.log(Level.FINE,"Reading model "+model.isEmpty());
            
            GraphManager.createResourceGraphs(graph,model.getNsPrefixMap());

            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        logger.log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  */
    
  @PUT
  @ApiOperation(value = "Create new graph and update service description", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response putJson(
          @ApiParam(value = "New model in application/ld+json", required = true) String body, 
          @ApiParam(value = "Model ID (graph)", required = true) 
          @QueryParam("graph") String graph,
          @ApiParam(value = "Group", required = true) 
          @QueryParam("group") String group) {
      
       if(graph.equals("default")) {
           return Response.status(403).build();
       }
       
       IRI graphIRI, namespaceIRI;
       
       try {
            IRIFactory iri = IRIFactory.semanticWebImplementation();
            graphIRI = iri.construct(graph);
            namespaceIRI = iri.construct(graph+"#");
        } catch (IRIException e) {
            logger.log(Level.WARNING, "Graph is invalid IRI!");
            return Response.status(403).build();
        }
       
       logger.info(graphIRI.toString());
       logger.info(namespaceIRI.toString());

       try {
 
            ServiceDescriptionManager.createGraphDescription(graph, group, null);
      
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.put(ClientResponse.class,body);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }

            logger.log(Level.INFO, graph+" updated sucessfully!");
            
            //JsonObject json = JSON.parse(response.getEntityInputStream());
            //ModelMaker modelMaker = ModelFactory.createMemModelMaker();
            
            Model model = ModelFactory.createDefaultModel(); //modelMaker.createModel(body);            
            RDFDataMgr.read(model, new ByteArrayInputStream(body.getBytes()), RDFLanguages.JSONLD);
            Map<String,String> prefixMap = model.getNsPrefixMap();
            
            GraphManager.updateModelNamespaceInfo(graph, namespaceIRI.toString(), model.getNsURIPrefix(namespaceIRI.toString()));
            
            GraphManager.createResourceGraphs(graph, prefixMap);
            
            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        logger.log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).build();
      }
  }
  
  @DELETE
  @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph")
  })
  public Response deleteJson(
          @ApiParam(value = "Graph to be deleted", required = true) 
          @QueryParam("graph") 
                String graph) {
      
    /*if(graph.equals("default")) {
           return Response.status(403).build();
     }*/

       try {

            GraphManager.deleteResourceGraphs(graph);
            
            Client client = Client.create();

            WebResource webResource = client.resource(services.getCoreReadWriteAddress())
                                      .queryParam("graph", graph);

            WebResource.Builder builder = webResource.header("Content-type", "application/ld+json");
            ClientResponse response = builder.delete(ClientResponse.class);

           if(!(graph.equals("undefined") || graph.equals("default"))) {
               ServiceDescriptionManager.deleteGraphDescription(graph);
           }
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, graph+" was not deleted! Status "+response.getStatus());
               return Response.status(response.getStatus()).build();
            }



            logger.log(Level.INFO, graph+" deleted successfully!");

            return Response.status(204).build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return Response.status(400).build();
      }
  }
  
  
  
  
}
