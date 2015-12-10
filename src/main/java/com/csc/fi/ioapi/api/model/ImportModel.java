/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.ImportManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.ClientResponse;
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
import javax.ws.rs.core.Response;
 
/**
 * Root resource (exposed at "importModel" path)
 */
@Path("importModel")
@Api(value = "/importModel", description = "Import existing model")
public class ImportModel {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ImportModel.class.getName());
    
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
      
      /* TODO: Add API key? */
      
       if(graph.equals("default")) {
           return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
       }
       
       IRI graphIRI, namespaceIRI;
       
       try {
            IRIFactory iri = IRIFactory.semanticWebImplementation();
            graphIRI = iri.construct(graph);
            namespaceIRI = iri.construct(graph+"#");
        } catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
       
       ServiceDescriptionManager.createGraphDescription(graph, group, null);

       /* Create new graph with the graph id */ 
       ClientResponse response = JerseyFusekiClient.putGraphToTheService(graph, body, services.getCoreReadWriteAddress());

       if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, "Unexpected: import failed: "+graph);
               return Response.status(response.getStatus()).entity(ErrorMessage.UNEXPECTED).build();
       }

        logger.log(Level.INFO, graph+" updated sucessfully!");

        //JsonObject json = JSON.parse(response.getEntityInputStream());
        //ModelMaker modelMaker = ModelFactory.createMemModelMaker();

        Model model = ModelFactory.createDefaultModel(); //modelMaker.createModel(body);            
        RDFDataMgr.read(model, new ByteArrayInputStream(body.getBytes()), RDFLanguages.JSONLD);
        Map<String,String> prefixMap = model.getNsPrefixMap();

        ImportManager.updateModelNamespaceInfo(graph, namespaceIRI.toString(), model.getNsURIPrefix(namespaceIRI.toString()));
        ImportManager.createResourceGraphs(graph, prefixMap);

        return Response.status(204).build();

  }
  
}
