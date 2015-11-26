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
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
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
          @ApiParam(value = "Graph id", defaultValue="default") 
          @QueryParam("id") String id,
          @ApiParam(value = "group")
          @QueryParam("group") String group) {

          String queryString;
          ParameterizedSparqlString pss = new ParameterizedSparqlString();
          
          if((group==null || group.equals("undefined")) && (id!=null && !id.equals("undefined") && !id.equals("default"))) {
              
            IRI modelIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    modelIRI = iri.construct(id);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).build();
            }
            
            /* TODO: Create Namespace service? */
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
            Model model = accessor.getModel(id);
            pss.setNsPrefixes(model.getNsPrefixMap());
            
             queryString = "CONSTRUCT { "
                     + "?graph a owl:Ontology . "
                     + "?graph rdfs:label ?label . "
                     + "?graph dcap:preferredXMLNamespaceName ?namespace . "
                     + "?graph dcap:preferredXMLNamespacePrefix ?prefix . "
                     + "?graph dcterms:hasPart ?CoreComponent . "
                     + "?graph dcterms:references ?ref . "
                     + "?ref dcterms:title ?title . "
                     + "?ref dcterms:identifier ?refID . "
                     + "?graph dcterms:requires ?req . "
                     + "?req rdfs:label ?reqLabel . "
                     + "?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                     + "?graph dcterms:isPartOf ?group . "
                     + "?group rdfs:label ?groupLabel . "
                     + "} WHERE { "
                     + "GRAPH ?graph { "
                     + "?graph a owl:Ontology . "
                     + "?graph rdfs:label ?label . "
                     + "?graph dcap:preferredXMLNamespaceName ?namespace . "
                     + "?graph dcap:preferredXMLNamespacePrefix ?prefix . "
                     + "OPTIONAL { ?graph rdfs:comment ?comment . }"
                     + "OPTIONAL { ?graph dcterms:hasPart ?CoreComponent . }"
                     + "OPTIONAL {?graph dcterms:references ?ref . "
                     + "?ref dcterms:title ?title . "
                     + "?ref dcterms:identifier ?refID . }"
                     + "OPTIONAL { ?graph dcterms:requires ?req . "
                     + "?req rdfs:label ?reqLabel . "
                     + "?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "?req dcap:preferredXMLNamespacePrefix ?prefixes . }"
                     + "} GRAPH <urn:csc:iow:sd> { "
                     + "?metaGraph a sd:NamedGraph . "
                     + "?metaGraph sd:name ?graph . "
                     + "?metaGraph dcterms:isPartOf ?group . "
                     + "} GRAPH <urn:csc:groups> { "
                     + "?group a foaf:Group . "
                     + "?group rdfs:label ?groupLabel . "
                     + "}}";
             
             /* TODO: Do fixed query? Query is expanded to avoid namespace collisions */
             queryString = LDHelper.expandSparqlQuery(queryString, LDHelper.PREFIX_MAP);
             
             logger.info(queryString);
             
             pss.setIri("graph", modelIRI);
             
     } else if(group!=null && !group.equals("undefined")) {
         
         
           IRI groupIRI;
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    groupIRI = iri.construct(group);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).build();
            }
             pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            /* IF group parameter is available list of core vocabularies is created */
             queryString = "CONSTRUCT { ?graphName rdfs:label ?label . ?graphName ?p ?o . ?graphName dcterms:identifier ?g . ?graphName dcterms:isPartOf ?group . ?graphName a sd:NamedGraph . } WHERE { ?graph sd:name ?graphName . ?graph a sd:NamedGraph ; dcterms:isPartOf ?group . GRAPH ?graphName { ?g a owl:Ontology . ?g rdfs:label ?label . }}"; 
             pss.setIri("group", groupIRI);
             
           } else {
             pss.setNsPrefixes(LDHelper.PREFIX_MAP);
             /* IF ID is null or default and no group available */
             queryString = "CONSTRUCT { ?g a owl:Ontology . ?g rdfs:label ?label . ?g dcap:preferredXMLNamespaceName ?namespace . ?g dcap:preferredXMLNamespacePrefix ?prefix . } "+
                           "WHERE { GRAPH ?g { ?g a owl:Ontology . ?g rdfs:label ?label . ?g dcap:preferredXMLNamespaceName ?namespace . ?g dcap:preferredXMLNamespacePrefix ?prefix . }}"; 
           }
           
            
            pss.setCommandText(queryString);
           
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());

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
          @Context HttpServletRequest request) {
      
       if(graph.equals("default") || graph.equals("undefined")) {
            return Response.status(403).build();
       } 
 
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).entity("{\"errorMessage\":\"Unauthorized\"}").build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditModel(graph))
            return Response.status(401).entity("{\"errorMessage\":\"Unauthorized\"}").build();
        
        String service = services.getCoreReadWriteAddress();
        ServiceDescriptionManager.updateGraphDescription(graph);

        ClientResponse response = JerseyFusekiClient.putGraphToTheService(graph, body, service);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           Logger.getLogger(Models.class.getName()).log(Level.WARNING, graph+" was not updated! Status "+response.getStatus());
           return Response.status(response.getStatus()).entity("{\"errorMessage\":\"Resource was not updated\"}").build();
        }

        Logger.getLogger(Models.class.getName()).log(Level.INFO, graph+" updated sucessfully!");

        return Response.status(204).build();

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
            return Response.status(403).entity("{\"errorMessage\":\"Invalid id\"}").build();
        }
             
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(401).entity("{\"errorMessage\":\"Unauthorized\"}").build();
        
        LoginSession login = new LoginSession(session);
        
        if(!login.isLoggedIn() || !login.hasRightToEditGroup(group))
            return Response.status(401).entity("{\"errorMessage\":\"Unauthorized\"}").build();
        
        /* TODO: CHECK IF GRAPH ALREADY EXISTS */ 
        
        if(!graph.equals("undefined")) {
            ServiceDescriptionManager.createGraphDescription(graph, group, login.getEmail());
        }
           
            String service = services.getCoreReadWriteAddress();
            ClientResponse response = JerseyFusekiClient.putGraphToTheService(graph, body, service);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(Models.class.getName()).log(Level.WARNING, graph+" was not created! Status "+response.getStatus());
               return Response.status(response.getStatus()).entity("{\"errorMessage\":\"Resource was not created\"}").build();
            }

            Logger.getLogger(Models.class.getName()).log(Level.INFO, graph+" updated sucessfully!");
           
            return Response.status(204).build();

  }
  
}
