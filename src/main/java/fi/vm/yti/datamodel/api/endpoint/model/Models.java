/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.system.Txn;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("model")
@Api(tags = {"Model"}, description = "Operations about models")
public class Models {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Models.class.getName());
   
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get model from service", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
          @ApiParam(value = "Graph id") 
          @QueryParam("id") String id,
          @ApiParam(value = "Service category")
          @QueryParam("serviceCategory") String group,
          @ApiParam(value = "prefix")
          @QueryParam("prefix") String prefix) {

          String queryString = QueryLibrary.modelQuery;

          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          
          if((id==null || id.equals("undefined")) && (prefix!=null && !prefix.equals("undefined"))) {
              logger.info("Resolving prefix: "+prefix);
                 id = GraphManager.getServiceGraphNameWithPrefix(prefix);
                 if(id==null) {
                        logger.log(Level.WARNING, "Invalid prefix: "+prefix);
                       return JerseyResponseManager.invalidIRI();
                 }
           }             
                       
          if((group==null || group.equals("undefined")) && (id!=null && !id.equals("undefined") && !id.equals("default"))) {
            logger.info("Model id:"+id);
            IRI modelIRI;
            
                try {
                        modelIRI = IDManager.constructIRI(id);
                } catch (IRIException e) {
                        logger.log(Level.WARNING, "ID is invalid IRI!");
                       return JerseyResponseManager.invalidIRI();
                }

                if(id.startsWith("urn:")) {
                   return JerseyClient.getGraphResponseFromService(id, services.getProvReadWriteAddress());
                }


            String sparqlService = services.getCoreSparqlAddress();
            String graphService = services.getCoreReadWriteAddress();

            /* TODO: Create Namespace service? */
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(graphService);
            Model model = accessor.getModel(id);

            //model.write(System.out, "text/turtle");
            
            if(model==null) {
                return JerseyResponseManager.notFound();
            }
            
            pss.setNsPrefixes(model.getNsPrefixMap());
            
            pss.setIri("graph", modelIRI);
            
            pss.setCommandText(queryString);
            //logger.info(pss.toString());
           
            return JerseyClient.constructGraphFromService(pss.toString(), sparqlService);
             
     } else  {

              pss.setNsPrefixes(LDHelper.PREFIX_MAP);
              queryString = QueryLibrary.modelsByGroupQuery;

             if(group!=null && !group.equals("undefined")) {
                  pss.setLiteral("groupCode", group);
              }

           }


            pss.setCommandText(queryString);
            
            return JerseyClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

  }
   
    /**
     * Replaces Graph in given service
     * @returns empty Response
     */
  @POST
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
          @ApiParam(value = "Updated model in application/ld+json", required = true) 
                String body, 
                @ApiParam(value = "Model ID")
                @QueryParam("id") 
                String graph,
          @Context HttpServletRequest request) {

      HttpSession session = request.getSession();

      LoginSession login = new LoginSession(session);

      if(!login.isLoggedIn()) {
          return JerseyResponseManager.unauthorized();
      }

      try {

          DataModel newVocabulary = new DataModel(body);

          logger.info("Getting old vocabulary:" + newVocabulary.getId());
          DataModel oldVocabulary = new DataModel(newVocabulary.getIRI());

          if (!login.isSuperAdmin()) {
              if (!login.isUserInOrganization(oldVocabulary.getOrganizations())) {
                  logger.info("User is not existing organisations");
                  return JerseyResponseManager.unauthorized();
              }
              if (!login.isUserInOrganization(newVocabulary.getOrganizations())) {
                  logger.info("User is not new models organisations");
                  return JerseyResponseManager.unauthorized();
              }
         } else {
              logger.info("Updating "+graph+" as superuser");
          }

          UUID provUUID = UUID.fromString(newVocabulary.getProvUUID().replaceFirst("urn:uuid:",""));

          if (provUUID == null) {
              return JerseyResponseManager.error();
          } else {

              newVocabulary.update();

              if(ProvenanceManager.getProvMode()) {
                 // ProvenanceManager.createProvenanceGraphFromModel(newVocabulary.getId(), newVocabulary.asGraph(), login.getEmail(), newVocabulary.getProvUUID());
                 // ProvenanceManager.createProvEntity(newVocabulary.getId(), login.getEmail(), newVocabulary.getProvUUID());
                  ProvenanceManager.createProvEntityBundle(newVocabulary.getId(), newVocabulary.asGraph(), login.getEmail(), newVocabulary.getProvUUID(), null);
              }

              return JerseyResponseManager.successUuid(provUUID);
          }

      } catch(IllegalArgumentException ex) {
          logger.warning(ex.toString());
          return JerseyResponseManager.error();
      }

  }
  
  @PUT
  @ApiOperation(value = "Create new model and update service description", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "New model created"),
      @ApiResponse(code = 200, message = "Bad request"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 403, message = "Overwrite is forbidden"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response putJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) String body,
          @Context HttpServletRequest request) {

      HttpSession session = request.getSession();

      LoginSession login = new LoginSession(session);

      if(!login.isLoggedIn()) {
          return JerseyResponseManager.unauthorized();
      }

      try {

          DataModel newVocabulary = new DataModel(body);

          if (!login.isSuperAdmin()) {
              if(!login.isUserInOrganization(newVocabulary.getOrganizations())) {
                  logger.info("User is not in organization");
                  return JerseyResponseManager.unauthorized();
              }
          } else {
              logger.info("Creating "+newVocabulary.getId()+" as superuser");
          }

          if(GraphManager.isExistingGraph(newVocabulary.getId())) {
              return JerseyResponseManager.usedIRI(newVocabulary.getId());
          }

          String provUUID = newVocabulary.getProvUUID();

          if (provUUID == null) {
              return JerseyResponseManager.serverError();
          }
          else {
              logger.info("Storing new model: "+newVocabulary.getId());

              newVocabulary.create();

              ServiceDescriptionManager.createGraphDescription(newVocabulary.getId(), login.getEmail(), newVocabulary.getOrganizations());

              if (ProvenanceManager.getProvMode()) {
                  ProvenanceManager.createProvenanceActivityFromModel(newVocabulary.getId(), newVocabulary.asGraph(), newVocabulary.getProvUUID(), login.getEmail());
              }

              logger.info("Created new model: "+newVocabulary.getId());
              return JerseyResponseManager.successUuid(provUUID,newVocabulary.getId());
          }

      } catch(IllegalArgumentException ex) {
          logger.warning(ex.toString());
          return JerseyResponseManager.error();
      }

  }
  
  @DELETE
  @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 406, message = "Not acceptable")
  })
  public Response deleteModel(
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("id") String id,
          @Context HttpServletRequest request) {
     
       /* Check that URIs are valid */
      IRI modelIRI;
        try {
            modelIRI = IDManager.constructIRI(id);
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        } catch(NullPointerException ex) {
            return JerseyResponseManager.invalidParameter();
        }
       
       HttpSession session = request.getSession();

       if(!GraphManager.isExistingGraph(modelIRI)) {
           return JerseyResponseManager.notFound();
       }

       LoginSession login = new LoginSession(session);

      if(!login.isSuperAdmin()) {
          if (!login.isLoggedIn()) {
              return JerseyResponseManager.unauthorized();
          }
      }

       if(!ApplicationProperties.getDebugMode() && GraphManager.modelStatusRestrictsRemoving(modelIRI)) {
          return JerseyResponseManager.cannotRemove();
       }

       DataModel deleteModel = new DataModel(modelIRI);

      if(!login.hasRightToEditModel(deleteModel.getOrganizations())) {
          return JerseyResponseManager.unauthorized();
      }


       deleteModel.delete();
       
       return JerseyResponseManager.ok();
    }
  
}
