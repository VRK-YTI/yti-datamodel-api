/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.DELETE;


/**
 * Root resource (exposed at "myresource" path)
 */
@Path("predicate")
@Api(tags = {"Predicate"}, description = "Operations about reusable properties")
public class Predicate {

    private static final Logger logger = Logger.getLogger(Predicate.class.getName());

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
 
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get property from model", notes = "More notes about this method")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
      @ApiParam(value = "Property id")
      @QueryParam("id") String id,
      @ApiParam(value = "Model id")
      @QueryParam("model") String model) {

      if(id==null || id.equals("undefined") || id.equals("default")) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = QueryLibrary.listPredicatesQuery;

         if(model!=null && !model.equals("undefined")) {
              pss.setIri("library", model);
              pss.setIri("hasPartGraph",model+"#HasPartGraph");
         }

        pss.setCommandText(queryString);
        
        return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

      } else {

            if(IDManager.isInvalid(id)) {
                return JerseyResponseManager.invalidIRI();
            }         
            
            if(id.startsWith("urn:")) {
               return JerseyJsonLDClient.getGraphResponseFromService(id, services.getProvReadWriteAddress());
            }   
           
            String sparqlService = services.getCoreSparqlAddress();
            String graphService = services.getCoreReadWriteAddress();       

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */
            Map<String, String> namespaceMap = NamespaceManager.getCoreNamespaceMap(id, graphService);

            if(namespaceMap==null) {
                logger.info("No model for "+id);
                return JerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.predicateQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }

          //  logger.info(pss.toString());
            return JerseyJsonLDClient.constructNotEmptyGraphFromService(pss.toString(), sparqlService);         

      }

  }
   
  
  @POST
  @ApiOperation(value = "Create new property to certain model OR add reference from existing property to another model", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
          @ApiParam(value = "New graph in application/ld+json", required = false) 
                String body, 
          @ApiParam(value = "Property ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "OLD Property ID") 
          @QueryParam("oldid") 
                String oldid,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {


      HttpSession session = request.getSession();

      if(session==null) return JerseyResponseManager.unauthorized();

      LoginSession login = new LoginSession(session);

      if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return JerseyResponseManager.unauthorized();

                
        IRI modelIRI,idIRI,oldIdIRI = null; 
        
       /* Check that URIs are valid */
        try {
            modelIRI = IDManager.constructIRI(model);
            idIRI = IDManager.constructIRI(id);
            /* If oldid exists */
            if(oldid!=null && !oldid.equals("undefined")) {
                if(oldid.equals(id)) {
                  /* id and oldid cant be the same */
                  return JerseyResponseManager.usedIRI();
                }
                oldIdIRI = IDManager.constructIRI(oldid);
            }
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
        
         String provUUID = null;
        
        if(isNotEmpty(body)) {

            ReusablePredicate updatePredicate = new ReusablePredicate(body);

            if(!login.isUserInOrganization(updatePredicate.getOrganizations())) {
                logger.info("User is not in organization");
                return JerseyResponseManager.unauthorized();
            }

            /* Rename ID if oldIdIRI exists */
            if(oldIdIRI!=null) {
                /* Prevent overwriting existing resources */
                if(GraphManager.isExistingGraph(idIRI)) {
                    logger.log(Level.WARNING, idIRI+" is existing graph!");
                    return JerseyResponseManager.usedIRI();
                } else {
                    updatePredicate.save();
                    provUUID = updatePredicate.getProvUUID();
                   // provUUID = ResourceManager.updateResourceWithNewId(idIRI, oldIdIRI, modelIRI, body, login);
                    GraphManager.updatePredicateReferencesInModel(modelIRI, oldIdIRI, idIRI);
                    logger.info("Changed id from:"+oldid+" to "+id);
                }
            } else {
                updatePredicate.save();
                provUUID = updatePredicate.getProvUUID();
              //  provUUID = ResourceManager.updateResource(id, model, body, login);
            }
        } else {
             /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
            if(LDHelper.isResourceDefinedInNamespace(id, model)) {
                // Selfreferences not allowed
                return JerseyResponseManager.usedIRI();
            } else {
                GraphManager.insertExistingGraphReferenceToModel(id, model);
                GraphManager.createExportGraphInRunnable(model);
                ConceptMapper.addConceptFromReferencedResource(model,id);
                return JerseyResponseManager.ok();
            }
        }
        
        if(provUUID!=null) {
            return JerseyResponseManager.successUuid(provUUID);
        }
        else return JerseyResponseManager.notCreated();
        
  }
  
  
  @PUT
  @ApiOperation(value = "Create new property to certain model", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Graph is created"),
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response putJson(
          @ApiParam(value = "New graph in application/ld+json", required = true) String body,
          @Context HttpServletRequest request) {

      try {
          HttpSession session = request.getSession();

          if(session==null) return JerseyResponseManager.unauthorized();

          LoginSession login = new LoginSession(session);

          if(!login.isLoggedIn()) {
              return JerseyResponseManager.unauthorized();
          }

          ReusablePredicate newPredicate = new ReusablePredicate(body);

            /* Prevent overwriting existing predicate */ 
            if(GraphManager.isExistingGraph(newPredicate.getId())) {
               logger.log(Level.WARNING, newPredicate.getId()+" is existing predicate!");
               return JerseyResponseManager.usedIRI();
            }

          if(!login.isUserInOrganization(newPredicate.getOrganizations())) {
              logger.info("User is not in organization");
              return JerseyResponseManager.unauthorized();
          }
           
           String provUUID = newPredicate.getProvUUID();
           newPredicate.save();

          if(ProvenanceManager.getProvMode()) {
              ProvenanceManager.createProvenanceGraphFromModel(newPredicate.getId(),newPredicate.asGraph(),login.getEmail(),newPredicate.getProvUUID());
          }
          
          if(provUUID!=null) return JerseyResponseManager.successUuid(provUUID,newPredicate.getId());
          else return JerseyResponseManager.notCreated();

      } catch(Exception ex) {
          Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.unexpected();
      }
  }
  
  
  @DELETE
  @ApiOperation(value = "Delete predicate graph or reference", notes = "Deletes predicate graph or reference")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph"),
      @ApiResponse(code = 401, message = "Unauthorized")
  })
  public Response deletePredicate(
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") String model,
          @ApiParam(value = "Predicate ID", required = true) 
          @QueryParam("id") String id,
          @Context HttpServletRequest request) {
      
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = IDManager.constructIRI(model);
            idIRI = IDManager.constructIRI(id);
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
      
       
       HttpSession session = request.getSession();

       if(session==null) return JerseyResponseManager.unauthorized();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return JerseyResponseManager.unauthorized();
       
        /* If Predicate is defined in the model */
        if(id.startsWith(model)) {
            /* Remove graph */
                Response resp = JerseyJsonLDClient.deleteGraphFromService(id, services.getCoreReadWriteAddress());
             /* TODO: Remove unused concepts?*/
             //   ConceptMapper.removeUnusedConcepts(model);
                return resp;
        } else {
            /* If removing referenced predicate */   
             GraphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);

             GraphManager.createExportGraphInRunnable(model);
            // ConceptMapper.removeUnusedConcepts(model);
             return JerseyResponseManager.ok();
           }
  }
  
 
}
