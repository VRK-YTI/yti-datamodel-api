/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.glassfish.jersey.uri.UriComponent;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("replicate")
@Api(tags = {"Admin"}, description = "Returns information about replicable models")
public class Replicator {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Replicator.class.getName());

    @GET
    @ApiOperation(value = "OK to replicate?")
    @Produces("application/json")
    public boolean getStatus(@Context HttpServletRequest request) {
      /* Add proper logic */  
      return true;
    }
   
    /**
     * Replaces Graph in given service
     * @returns empty Response
     */
  @PUT
  @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "IOW Service ID in form of http://domain/api/rest/ ", required = true) 
                @QueryParam("service") 
                String service,
                @Context HttpServletRequest request) {
      
      
       if(service==null || service.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
        HttpSession session = request.getSession();

        LoginSession login = new LoginSession(session);

        if(!login.isSuperAdmin()) {
            return JerseyResponseManager.unauthorized();
        }
        
        Boolean replicate = JerseyClient.readBooleanFromURL(service+"replicate");

        if(replicate!=null && replicate.booleanValue()) {
            logger.info("Replicating data from "+service);
        }

      try {

          Model modelList = JerseyClient.getResourceAsJenaModel(service + "serviceDescription");
          logger.info("Service description size: " + modelList.size());

          replicateServices(service, login);

          return JerseyResponseManager.okEmptyContent();

      } catch(Exception ex) {
            logger.warning(ex.getMessage());
            return JerseyResponseManager.error();
      }

  }

  public void replicateServices(String externalService, LoginSession login) {

      GraphManager.deleteGraphs();

      try (RDFConnection conn = RDFConnectionFactory.connect(services.getEndpoint()+"/core") ) {
          Txn.executeWrite(conn, ()-> {
              Dataset externalDataset = JerseyClient.getExternalJSONLDDatasets(externalService + "exportGraphs?service=core&content-type=application%2Ftrig");
              logger.info("Size of the CORE dataset: "+externalDataset.asDatasetGraph().size());
              conn.loadDataset(externalDataset);
          });
      } catch(Exception ex) {
          logger.warning(ex.getMessage());
      }

      try (RDFConnection conn = RDFConnectionFactory.connect(services.getEndpoint()+"/prov") ) {
          Txn.executeWrite(conn, ()-> {
              Dataset externalDataset = JerseyClient.getExternalJSONLDDatasets(externalService + "exportGraphs?service=prov&content-type=application%2Ftrig");
              logger.info("Size of the PROV dataset: "+externalDataset.asDatasetGraph().size());
              conn.loadDataset(externalDataset);
          });
      } catch(Exception ex) {
          logger.warning(ex.getMessage());
      }

  }

  
}
