/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

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
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ModelManager;
import com.csc.fi.ioapi.utils.QueryLibrary;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
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
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("model")
@Api(value = "/model", description = "Operations about models")
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
          @ApiParam(value = "group")
          @QueryParam("group") String group,
          @ApiParam(value = "prefix")
          @QueryParam("prefix") String prefix) {

          String queryString = QueryLibrary.modelQuery;
          
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          
          if((id==null || id.equals("undefined")) && (prefix!=null && !prefix.equals("undefined"))) { 
                 id = GraphManager.getServiceGraphNameWithPrefix(prefix);
                 if(id==null) {
                        logger.log(Level.WARNING, "Invalid prefix: "+prefix);
                       return JerseyResponseManager.invalidIRI();
                 }
           }             
                       
          if((group==null || group.equals("undefined")) && (id!=null && !id.equals("undefined") && !id.equals("default"))) {
            
            IRI modelIRI;
            
                try {
                        modelIRI = IDManager.constructIRI(id);
                } catch (IRIException e) {
                        logger.log(Level.WARNING, "ID is invalid IRI!");
                       return JerseyResponseManager.invalidIRI();
                }

                if(id.startsWith("urn:")) {
                   return JerseyJsonLDClient.getGraphResponseFromService(id, services.getProvReadWriteAddress());
                }
           
            String sparqlService = services.getCoreSparqlAddress();
            String graphService = services.getCoreReadWriteAddress();

            /* TODO: Create Namespace service? */
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(graphService);
            Model model = accessor.getModel(id);
            
            if(model==null) {
                return JerseyResponseManager.notFound();
            }
            
            pss.setNsPrefixes(model.getNsPrefixMap());
            
            pss.setIri("graph", modelIRI);
            
            pss.setCommandText(queryString);
            
           
            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), sparqlService);
             
     } else if(group!=null && !group.equals("undefined")) {
         
           IRI groupIRI;
            try {
                    groupIRI = IDManager.constructIRI(group);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return JerseyResponseManager.invalidIRI();
            }
             pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            /* IF group parameter is available list of core vocabularies is created */
             queryString = "CONSTRUCT { "
                     + "?graphName rdfs:label ?label . "
                     + "?graphName a ?type . "
                     + "?graphName dcterms:isPartOf ?group . "
                     + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                     + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                     + "?group a foaf:Group . "
                     + "?group dcterms:references ?skosScheme . "
                     + "?skosScheme skos:prefLabel ?schemeTitle . "
                     + "?skosScheme termed:graph ?termedGraph . "
                     + "?termedGraph termed:id ?termedGraphId . "
                     + "?termedGraph termed:code ?termedGraphCode . "
                     + "?group rdfs:label ?groupLabel . "
                     + "} WHERE { "
                     + "GRAPH <urn:csc:iow:sd> {"
                     + "?graph sd:name ?graphName . "
                     + "?graph a sd:NamedGraph . "
                     + "?graph dcterms:isPartOf ?group . "
                     + "}"
                     + "GRAPH <urn:csc:groups> { "
                     + "?group rdfs:label ?groupLabel . "
                     + "?group dcterms:references ?skosScheme . "
                     + "?skosScheme dcterms:title ?schemeTitle . "
                     + "?skosScheme termed:graph ?termedGraph . "
                     + "?termedGraph termed:id ?termedGraphId . "
                     + "?termedGraph termed:code ?termedGraphCode . "
                     + "}"
                     + "GRAPH ?graphName { "
                     + "?graphName a ?type . "
                     + "?graphName rdfs:label ?label . "
                     + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                     + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                     + "}}";
             
             pss.setIri("group", groupIRI);
                     
           } else {
             pss.setNsPrefixes(LDHelper.PREFIX_MAP);
             /* IF ID is null or default and no group available */
             queryString = "CONSTRUCT { "
                     + "?g a ?type . ?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . } "
                     + "WHERE { "
                     + "GRAPH ?g { "
                     + "?g a ?type . "
                     + "?g rdfs:label ?label . "
                     + "?g dcap:preferredXMLNamespaceName ?namespace . "
                     + "?g dcap:preferredXMLNamespacePrefix ?prefix . }}"; 
           }
           
            
            pss.setCommandText(queryString);
            
            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

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
          @ApiParam(value = "Model ID", required = true) 
                @QueryParam("id") 
                String graph,
                @ApiParam(value = "version flag", defaultValue="false") 
                @QueryParam("version") 
                boolean version,
          @Context HttpServletRequest request) {
      
       if(graph.equals("default") || graph.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
       if(IDManager.isInvalid(graph)) {
           return JerseyResponseManager.invalidIRI();
       }
 
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(graph))
            return JerseyResponseManager.unauthorized();
        
        UUID provUUID = ModelManager.updateModel(graph, body, login);
        
        if(provUUID==null) return JerseyResponseManager.error();
        else return JerseyResponseManager.successUuid(provUUID);

  }
  
  @PUT
  @ApiOperation(value = "Create new graph and update service description", notes = "PUT Body should be json-ld")
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
          @ApiParam(value = "Model ID", required = true) 
          @QueryParam("id") 
                String graph,
          @ApiParam(value = "Group", required = true) 
          @QueryParam("group") 
                String group,
          @Context HttpServletRequest request) {
      
        if(graph.equals("default")) {
            return JerseyResponseManager.invalidIRI();
        }
        
       IRI graphIRI;
       
            try {
                graphIRI = IDManager.constructIRI(graph);
            } catch (IRIException e) {
                logger.log(Level.WARNING, "GRAPH ID is invalid IRI!");
                return JerseyResponseManager.invalidIRI();
            } 
            
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditGroup(group))
            return JerseyResponseManager.unauthorized();
        
            if(GraphManager.isExistingGraph(graphIRI)) {
                return JerseyResponseManager.usedIRI();
            }
        
            UUID provUUID = ModelManager.createNewModel(graph, group, body, login);
        
            if(provUUID==null) return JerseyResponseManager.error();
            else return JerseyResponseManager.successUuid(provUUID);
            
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
        }
       
       HttpSession session = request.getSession();

       if(!GraphManager.isExistingGraph(modelIRI)) {
           return JerseyResponseManager.notFound();
       }
       
       if(session==null) return JerseyResponseManager.unauthorized();

       LoginSession login = new LoginSession(session);

       if(!login.isLoggedIn() || !login.hasRightToEditModel(id))
          return JerseyResponseManager.unauthorized();
       
       if(GraphManager.modelStatusRestrictsRemoving(modelIRI)) {
          return JerseyResponseManager.cannotRemove();
       }
       
       ServiceDescriptionManager.deleteGraphDescription(id);  
       GraphManager.removeModel(modelIRI);
       
       return JerseyResponseManager.ok();
    }
  
}
