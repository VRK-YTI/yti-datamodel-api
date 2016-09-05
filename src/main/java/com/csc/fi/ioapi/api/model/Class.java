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
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
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
      @QueryParam("model") String model,
      @ApiParam(value = "prefix")
      @QueryParam("prefix") String prefix) {

      
        if(prefix!=null || !prefix.equals("undefined")) { 
         String namespace = GraphManager.getServiceGraphNameWithPrefix(prefix);
             if(id==null) {
                    logger.log(Level.WARNING, "Invalid prefix: "+prefix);
                   return Response.status(403).entity(ErrorMessage.INVALIDPREFIX).build();
             }
          id = namespace+"#"+id;
        }    
      
      if(id==null || id.equals("undefined") || id.equals("default")) {
          
        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?class rdfs:label ?label . "
                + "?class a ?type . "
                + "?class dcterms:modified ?modified . "
                + "?class rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabel . "
                + "?source a ?sourceType . "
                + "} WHERE { "
                + "GRAPH ?hasPartGraph { "
                + "?library dcterms:hasPart ?class . } "
                + "GRAPH ?class { "
                + "?class dcterms:modified ?modified . "
                + "?class rdfs:label ?label . "
                + "?class a ?type . "
                + "VALUES ?type { rdfs:Class sh:Shape } "
                + "?class rdfs:isDefinedBy ?source .  } "
                + "GRAPH ?source { "
                + "?source a ?sourceType . "
                + "?source rdfs:label ?sourceLabel . "
                + "} }"; 
        

         if(model!=null && !model.equals("undefined")) {
              pss.setIri("library", model);
              pss.setIri("hasPartGraph",model+"#HasPartGraph");
         }

        pss.setCommandText(queryString);

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

      } else {
          
            IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
            IRI idIRI;
            try {
                idIRI = iriFactory.construct(id);
            }
            catch (IRIException e) {
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
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
                return Response.status(404).entity(ErrorMessage.NOTFOUND).build();
            }

            pss.setNsPrefixes(namespaceMap);

            String queryString = QueryLibrary.classQuery;
            pss.setCommandText(queryString);

            pss.setIri("graph", id);

            
            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }

           // logger.info(pss.toString());
            
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
        
        if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
            return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
                
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI,oldIdIRI = null;        
        
        /* Check that URIs are valid */
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
            /* If newid exists */
            if(oldid!=null && !oldid.equals("undefined")) {
                if(oldid.equals(id)) {
                  /* id and newid cant be the same */
                  return Response.status(403).entity(ErrorMessage.USEDIRI).build();
                }
                oldIdIRI = iriFactory.construct(oldid);
            }
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
        
        UUID provUUID = UUID.randomUUID();
        
        if(isNotEmpty(body)) {
            
            /* Rename ID if oldIdIRI exists */
            if(oldIdIRI!=null) {
                /* Prevent overwriting existing resources */
                if(GraphManager.isExistingGraph(idIRI)) {
                    logger.log(Level.WARNING, idIRI+" is existing graph!");
                    return Response.status(403).entity(ErrorMessage.USEDIRI).build();
                }
                else {
                    /* Remove old graph and add update references */
                    GraphManager.removeGraph(oldIdIRI);
                    GraphManager.renameID(oldIdIRI,idIRI);
                    if(ProvenanceManager.getProvMode()) {
                        ProvenanceManager.renameID(oldid,id);
                    }
                }
            }
            
           /* Create new graph with new id */ 
           ClientResponse response = JerseyFusekiClient.putGraphToTheService(id, body, services.getCoreReadWriteAddress());

           ConceptMapper.addConceptFromReferencedResource(model,id);
           
           GraphManager.updateModifyDates(id);
           
           if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               /* TODO: Create prov events from failed updates? */
               logger.log(Level.WARNING, "Unexpected: Not updated: "+id);
               return Response.status(response.getStatus()).entity(ErrorMessage.UNEXPECTED).build();
           } 
            
           /* If update is successfull create new prov entity */ 
           if(ProvenanceManager.getProvMode()) {
                ProvenanceManager.createProvenanceGraph(id, body, login.getEmail(), provUUID); 
           }

        } else {
             /* IF NO JSON-LD POSTED TRY TO CREATE REFERENCE FROM MODEL TO CLASS ID */
            if(id.startsWith(model)) {
                // Selfreferences not allowed
                Response.status(403).entity(ErrorMessage.USEDIRI).build();
            } else {
                GraphManager.insertExistingGraphReferenceToModel(id, model);
                ConceptMapper.addConceptFromReferencedResource(model,id);
                return Response.status(204).build();
            }
        }
        
        GraphManager.createExportGraph(model);
        
        Logger.getLogger(Class.class.getName()).log(Level.INFO, id+" updated sucessfully");
        return Response.status(204).entity("{\"identifier\":\"urn:uuid:"+provUUID+"\"}").build();
        
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
        
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI,idIRI;   
        
        /* Check that URIs are valid */
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch(NullPointerException e) {
            return Response.status(403).entity(ErrorMessage.UNEXPECTED).build();
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
               
        HttpSession session = request.getSession();

        if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

        LoginSession login = new LoginSession(session);

            if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
                return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

             if(!id.startsWith(model)) {
                Logger.getLogger(Class.class.getName()).log(Level.WARNING, id+" ID must start with "+model);
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
             }

            /* Prevent overwriting existing classes */ 
            if(GraphManager.isExistingGraph(idIRI)) {
               logger.log(Level.WARNING, idIRI+" is existing class!");
               return Response.status(403).entity(ErrorMessage.USEDIRI).build();
            }
          
           /* Create new graph with new id */ 
           ClientResponse response = JerseyFusekiClient.putGraphToTheService(id, body, services.getCoreReadWriteAddress());

           if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, "Unexpected: Not created: "+id);
               return Response.status(response.getStatus()).entity(ErrorMessage.UNEXPECTED).build();
           }


            UUID provUUID = UUID.randomUUID();
            
           /* If new class was created succesfully create prov activity */
           if(ProvenanceManager.getProvMode()) {
               ProvenanceManager.createProvenanceActivity(id, login.getEmail(), body, provUUID);
           }
            
            GraphManager.insertNewGraphReferenceToModel(id, model);
            
            logger.log(Level.INFO, id+" updated sucessfully!");
            
            ConceptMapper.addConceptFromReferencedResource(model,id);
            
            JerseyFusekiClient.postGraphToTheService(model+"#ExportGraph", body, services.getCoreReadWriteAddress());
            
          return Response.status(204).entity("{\"identifier\":\"urn:uuid:"+provUUID+"\"}").build();

      } catch(UniformInterfaceException | ClientHandlerException ex) {
        Logger.getLogger(Class.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
        return Response.status(400).entity(ErrorMessage.UNEXPECTED).build();
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
      
      IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
       /* Check that URIs are valid */
      IRI modelIRI,idIRI;
        try {
            modelIRI = iriFactory.construct(model);
            idIRI = iriFactory.construct(id);
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
      
       HttpSession session = request.getSession();

       if(session==null) return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(model))
          return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
       
       /* If Class is defined in the model */
       if(id.startsWith(model)) {
           /* Remove graph */
            Response resp = JerseyFusekiClient.deleteGraphFromService(id, services.getCoreReadWriteAddress());   
           // ConceptMapper.removeUnusedConcepts(model);
            return resp;
        } else {
        /* If removing referenced class */   
        /* TODO: Add response to GraphManager? */   
             GraphManager.deleteGraphReferenceFromModel(idIRI,modelIRI);
             GraphManager.createExportGraph(model);
          // ConceptMapper.removeUnusedConcepts(model);
             return Response.status(204).build();   
       }
  }

}
