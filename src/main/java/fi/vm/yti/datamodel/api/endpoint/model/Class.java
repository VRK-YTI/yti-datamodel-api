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

import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import javax.ws.rs.DELETE;
 
/**
 * Root resource (exposed at "class" path)
 */
@Path("class")
@Api(tags = {"Class"}, description = "Class operations")
public class Class {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Class.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get class from model", notes = "Get class in JSON-LD")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "No such resource"),
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
      @ApiParam(value = "Class id")
      @QueryParam("id") String id,
      @ApiParam(value = "Model id")
      @QueryParam("model") String model) {

      if(id==null || id.equals("undefined") || id.equals("default")) {
          
        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = QueryLibrary.listClassesQuery;

         if(model!=null && !model.equals("undefined")) {
              pss.setIri("library", model);
              pss.setIri("hasPartGraph",model+"#HasPartGraph");
         }

        pss.setCommandText(queryString);

        return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

      } else {
          

            if(!IDManager.isValidUrl(id)) {
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
                return JerseyResponseManager.notFound();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.classQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            
            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }

            return JerseyJsonLDClient.constructNotEmptyGraphFromService(pss.toString(), sparqlService);         

      }
         
  }
 
  
  @POST
  @ApiOperation(value = "Update class in certain model OR add reference from existing class to another model AND/OR change Class ID", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Class ID", required = true)
          @QueryParam("id")
                String id,
          @ApiParam(value = "OLD Class ID") 
          @QueryParam("oldid")
                String oldid,
          @ApiParam(value = "Model ID", required = true)
          @QueryParam("model")
                String model,
          @Context HttpServletRequest request) {

      try {
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
            /* If newid exists */
            if(oldid!=null && !oldid.equals("undefined")) {
                if(oldid.equals(id)) {
                  /* id and newid cant be the same */
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

            ReusableClass updateClass = new ReusableClass(body);

            if(!login.isUserInOrganization(updateClass.getOrganizations())) {
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
                    updateClass.updateWithNewId(oldIdIRI);
                    provUUID = updateClass.getProvUUID();
                    logger.info("Changed class id from:"+oldid+" to "+id);
                }
            } else {
                updateClass.update();
                provUUID = updateClass.getProvUUID();
            }

            if(ProvenanceManager.getProvMode()) {
                if(oldIdIRI!=null) {
                    ProvenanceManager.renameID(oldIdIRI.toString(), idIRI.toString());
                }
                ProvenanceManager.createProvenanceGraphFromModel(updateClass.getId(), updateClass.asGraph(), login.getEmail(), updateClass.getProvUUID());
                ProvenanceManager.createProvEntity(updateClass.getId(), login.getEmail(), updateClass.getProvUUID());
            }


        } else {
             /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
   
            if(LDHelper.isResourceDefinedInNamespace(id, model)) {
                // Self references not allowed
                return JerseyResponseManager.usedIRI();
            } else {
                GraphManager.insertExistingGraphReferenceToModel(id, model);
                GraphManager.insertNewGraphReferenceToExportGraph(id, model);
                GraphManager.addCoreGraphToCoreGraph(id, model+"#ExportGraph");
                logger.info("Created reference from "+model+" to "+id);
                return JerseyResponseManager.ok();
            }
        }
        
        if(provUUID!=null) {
            return JerseyResponseManager.successUuid(provUUID);
        }
        else {
            return JerseyResponseManager.notCreated();
        }

      } catch(IllegalArgumentException ex) {
          logger.warning(ex.toString());
          return JerseyResponseManager.invalidParameter();
      } catch(Exception ex) {
          Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.unexpected();
      }
  }
 
  @PUT
  @ApiOperation(value = "Create new class to certain model", notes = "PUT Body should be json-ld")
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

        ReusableClass newClass = new ReusableClass(body);

        /* Prevent overwriting existing classes */
        if(GraphManager.isExistingGraph(newClass.getId())) {
               logger.log(Level.WARNING, newClass.getId()+" is existing class!");
               return JerseyResponseManager.usedIRI();
        }

        if(!login.isUserInOrganization(newClass.getOrganizations())) {
            logger.info("User is not in organization");
            return JerseyResponseManager.unauthorized();
        }

        String provUUID = newClass.getProvUUID();

        if (provUUID == null) {
            return JerseyResponseManager.serverError();
        }
        else {
            newClass.create();
            logger.info("Created "+newClass.getId());

            if (ProvenanceManager.getProvMode()) {
                ProvenanceManager.createProvenanceGraphFromModel(newClass.getId(), newClass.asGraph(), login.getEmail(), newClass.getProvUUID());
                ProvenanceManager.createProvenanceActivity(newClass.getId(), newClass.getProvUUID(),login.getEmail());
            }

            return JerseyResponseManager.successUuid(newClass.getProvUUID(), newClass.getId());
        }

      } catch(IllegalArgumentException ex) {
        logger.warning(ex.toString());
        return JerseyResponseManager.invalidParameter();
      } catch(Exception ex) {
        Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return JerseyResponseManager.unexpected();
      }
  }
 
  @DELETE
  @ApiOperation(value = "Delete graph from service and service description", notes = "Delete graph")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is deleted"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "No such graph"),
      @ApiResponse(code = 401, message = "Unauthorized")
  })
  public Response deleteClass(
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") String model,
          @ApiParam(value = "Class ID", required = true) 
          @QueryParam("id") String id,
          @Context HttpServletRequest request) {
      
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = IDManager.constructIRI(model);
            idIRI = IDManager.constructIRI(id);
        }
        catch(NullPointerException e) {
            return JerseyResponseManager.invalidIRI();
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
      
       HttpSession session = request.getSession();

       if(session==null) return JerseyResponseManager.unauthorized();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return JerseyResponseManager.unauthorized();
       
       /* If Class is defined in the model */
       if(id.startsWith(model)) {
           /* Remove graph */

           try {
               ReusableClass deleteClass = new ReusableClass(id);
               deleteClass.delete();
           } catch(IllegalArgumentException ex) {
               logger.warning(ex.toString());
               return JerseyResponseManager.unexpected();
           }
            // Response resp = JerseyJsonLDClient.deleteGraphFromService(id, services.getCoreReadWriteAddress());
            // GraphManager.createExportGraphInRunnable(model);
            // ConceptMapper.removeUnusedConcepts(model);
            return JerseyResponseManager.ok();
        } else {
           /* If removing referenced class */
             GraphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);

             GraphManager.deleteGraphReferenceFromExportModel(idIRI, modelIRI);
             // TODO: Not removed from export model

             return JerseyResponseManager.ok();
       }
  }

}
