/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.ImportManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.ServiceDescriptionManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
 
/**
 * Root resource (exposed at "importModel" path)
 */
@Path("importModel")
@Api(tags = {"Admin"}, description = "Import existing model")
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
          @ApiParam(value = "Service categories", required = true)
          @QueryParam("orgList") List<String> serviceList,
          @ApiParam(value = "Organization UUIDs", required = true)
          @QueryParam("orgList") List<UUID> orgList) {
      
      /* TODO: Add API key? */
      
       if(graph.equals("default")) {
           return JerseyResponseManager.invalidIRI();
       }
       
       IRI graphIRI, namespaceIRI;
       
       try {
            graphIRI = IDManager.constructIRI(graph);
            namespaceIRI = IDManager.constructIRI(graph+"#");
        } catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
       
       ServiceDescriptionManager.createGraphDescription(graph,null, orgList);

       /* Create new graph with the graph id */ 
       StatusType status = JerseyJsonLDClient.putGraphToTheService(graph, body, services.getCoreReadWriteAddress());

       if (status.getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, "Unexpected: import failed: "+graph);
               return JerseyResponseManager.unexpected(status.getStatusCode());
       }

        logger.log(Level.INFO, graph+" updated sucessfully!");

        //JsonObject json = JSON.parse(response.getEntityInputStream());
        //ModelMaker modelMaker = ModelFactory.createMemModelMaker();

        Model model = ModelFactory.createDefaultModel(); //modelMaker.createModel(body);            
        RDFDataMgr.read(model, new ByteArrayInputStream(body.getBytes()), RDFLanguages.JSONLD);
        Map<String,String> prefixMap = model.getNsPrefixMap();
        
        ImportManager.updateModelNamespaceInfo(graph, namespaceIRI.toString(), model.getNsURIPrefix(namespaceIRI.toString()));
        ImportManager.createResourceGraphs(graph, prefixMap);

        return JerseyResponseManager.okNoContent();

  }
  
}
