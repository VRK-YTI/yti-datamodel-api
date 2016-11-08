/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import com.csc.fi.ioapi.config.ApplicationProperties;
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
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ResourceManager;
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.NamespaceManager;
import com.csc.fi.ioapi.utils.ProvenanceManager;
import com.csc.fi.ioapi.utils.QueryLibrary;
import org.apache.jena.query.ParameterizedSparqlString;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.DELETE;
 
/**
 * Root resource (exposed at "class" path)
 */
@Path("class")
@Api(value = "/class", description = "Class operations")
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

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

      } else {
          
            IRIFactory iriFactory = IRIFactory.iriImplementation();
            IRI idIRI;
            try {
                idIRI = iriFactory.construct(id);
            }
            catch (IRIException e) {
                return JerseyResponseManager.invalidIRI();
            }  
            
            
            if(id.startsWith("urn:")) {
               return JerseyFusekiClient.getGraphResponseFromService(id, services.getProvReadWriteAddress());
            }   
           
            String sparqlService = services.getCoreSparqlAddress();
            String graphService = services.getCoreReadWriteAddress();

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            /* Get Map of namespaces from id-graph */
            Map<String, String> namespaceMap = NamespaceManager.getCoreNamespaceMap(id, graphService);

            if(namespaceMap==null) {
                return JerseyResponseManager.error();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.classQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            
            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }
            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), sparqlService);         

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
              
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
            return JerseyResponseManager.unauthorized();
                
        IRIFactory iriFactory = IRIFactory.iriImplementation();
        IRI modelIRI,idIRI,oldIdIRI = null;        
        
        /* Check that URIs are valid */
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
            /* If newid exists */
            if(oldid!=null && !oldid.equals("undefined")) {
                if(oldid.equals(id)) {
                  /* id and newid cant be the same */
                  return JerseyResponseManager.usedIRI();
                }
                oldIdIRI = iriFactory.construct(oldid);
            }
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
        
        UUID provUUID = null;
        
        if(isNotEmpty(body)) {
            
            /* Rename ID if oldIdIRI exists */
            if(oldIdIRI!=null) {
                /* Prevent overwriting existing resources */
                if(GraphManager.isExistingGraph(idIRI)) {
                    logger.log(Level.WARNING, idIRI+" is existing graph!");
                    return JerseyResponseManager.usedIRI();
                } else {
                    provUUID = ResourceManager.updateResourceWithNewId(idIRI, oldIdIRI, modelIRI, body, login);
                    GraphManager.updateClassReferencesInModel(modelIRI, oldIdIRI, idIRI);
                    logger.info("Changed id from:"+oldid+" to "+id);
                }
            } else {
                provUUID = ResourceManager.updateClass(id, model, body, login);
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
        else {
            return JerseyResponseManager.notCreated();
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
          @ApiParam(value = "New graph in application/ld+json", required = true) 
                String body, 
          @ApiParam(value = "Class ID", required = true) 
          @QueryParam("id") 
                String id,
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("model") 
                String model,
          @Context HttpServletRequest request) {
      
    try {
        
        IRIFactory iriFactory = IRIFactory.iriImplementation();
        IRI modelIRI,idIRI;   
        
        /* Check that URIs are valid */
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch(NullPointerException e) {
            return JerseyResponseManager.unexpected();
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
        }
               
        HttpSession session = request.getSession();

        if(session==null) return JerseyResponseManager.unauthorized();

        LoginSession login = new LoginSession(session);

            if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
                return JerseyResponseManager.unauthorized();

             if(!id.startsWith(model)) {
                Logger.getLogger(Class.class.getName()).log(Level.WARNING, id+" ID must start with "+model);
                return JerseyResponseManager.invalidIRI();
             }

            /* Prevent overwriting existing classes */ 
            if(GraphManager.isExistingGraph(idIRI)) {
               logger.log(Level.WARNING, idIRI+" is existing class!");
               return JerseyResponseManager.usedIRI();
            }
          
           UUID provUUID = ResourceManager.putNewResource(id, model, body, login);
          
           if(provUUID!=null) return JerseyResponseManager.successUuid(provUUID);
           else return JerseyResponseManager.notCreated();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
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
      
      IRIFactory iriFactory = IRIFactory.iriImplementation();
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
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
            Response resp = JerseyFusekiClient.deleteGraphFromService(id, services.getCoreReadWriteAddress());   
            GraphManager.createExportGraphInRunnable(model);
           // ConceptMapper.removeUnusedConcepts(model);
            return resp;
        } else {
        /* If removing referenced class */    
             GraphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);
             GraphManager.createExportGraphInRunnable(model);
        /* TODO: Remove unused concepts? */
        // ConceptMapper.removeUnusedConcepts(model);
             return JerseyResponseManager.ok();
       }
  }

}
