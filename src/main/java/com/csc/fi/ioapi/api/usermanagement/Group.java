package com.csc.fi.ioapi.api.usermanagement;

import com.csc.fi.ioapi.config.Endpoint;
import com.csc.fi.ioapi.utils.LDHelper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.DELETE;
import static javax.ws.rs.HttpMethod.POST;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("groups")
@Api(value = "/groups", description = "Edit groups")
public class Group {

    @Context ServletContext context;

    public String userEndpoint() {
       return Endpoint.getEndpoint() + "/users/data";
    }
    
    public String userSparqlEndpoint() {
       return Endpoint.getEndpoint() + "/users/sparql";
    }
    
    public String userSparqlUpdateEndpoint() {
       return Endpoint.getEndpoint() + "/users/update";
    }
    
    @POST
    @ApiOperation(value = "Update group", notes = "Add users or change name")
      @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Group is updated"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    public Response modifyGroup(@ApiParam(value = "Group ID", required = true) @QueryParam("group") String group, @ApiParam(value = "user") @QueryParam("user") String user, @ApiParam(value = "groupname") @QueryParam("groupname") String groupname) {
        
         IRI groupID;    
            try{
               IRIFactory iri = IRIFactory.semanticWebImplementation();
               groupID = iri.construct(group);
            } catch(IRIException e) {
               Logger.getLogger(User.class.getName()).log(Level.WARNING, "GROUP ID "+group+" is invalid IRI!");
               return Response.status(403).build();  
            }
        
        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        if(groupname!=null && !groupname.equals("undefined")) {
              queryString = "DELETE { GRAPH <urn:csc:groups> { ?group rdfs:label ?newname } } INSERT { GRAPH <urn:csc:groups> { ?group rdfs:label ?group . }} WHERE { GRAPH <urn:csc:groups> {?group a foaf:Group ; rdfs:label ?name . } }";
              pss.setCommandText(queryString);
              pss.setIri("newname", groupname);
        } else if(user!=null && !user.equals("undefined")) {
          
            IRI userID;    
            try{
               IRIFactory iri = IRIFactory.semanticWebImplementation();
               userID = iri.construct(group);
            } catch(IRIException e) {
               Logger.getLogger(User.class.getName()).log(Level.WARNING, "USER ID "+user+" is invalid IRI!");
               return Response.status(403).build();  
            }

          queryString = "INSERT { GRAPH <urn:csc:groups> { ?group foaf:member ?user . }} WHERE {GRAPH <urn:csc:groups> { ?group a foaf:Group .} }"; 
          pss.setCommandText(queryString);
          pss.setIri("user", userID);
          
        } else {
           return Response.status(403).build();
        }
        
        pss.setIri("group", groupID);
        
        UpdateRequest query = pss.asUpdate();
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(query,userSparqlUpdateEndpoint());
        
        try {
            qexec.execute();
            return Response.status(200).build();
        } catch (QueryExceptionHTTP ex) {
           Logger.getLogger(User.class.getName()).log(Level.WARNING, "Expect the unexpected!", ex);
           return Response.status(400).build(); 
        }              
    }
    
    
    @PUT
    @ApiOperation(value = "Add new group", notes = "PUT Body should be json-ld")
      @ApiResponses(value = {
      @ApiResponse(code = 204, message = "New group is created"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 404, message = "Service not found") 
  })
    public Response addNewGroup(@ApiParam(value = "Name", required = true) @QueryParam("name") String name) {
    
        UUID groupID = UUID.randomUUID();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String query = "INSERT { GRAPH <urn:csc:groups> { <urn:uuid:"+groupID+"> a foaf:Group ; rdfs:label ?name . } } WHERE { GRAPH <urn:csc:groups> { FILTER NOT EXISTS { ?any a foaf:Group ; rdfs:label ?name } } }";
        
        pss.setLiteral("name", name);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,userSparqlUpdateEndpoint());
        
        try {
            qexec.execute();
            return Response.status(200).build(); 
        } catch(Exception ex) {
            return Response.status(400).build(); 
        }
 
    }
    
    
      @GET
      @ApiOperation(value = "Get groups", notes = "")
      @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Graph is saved"),
        @ApiResponse(code = 400, message = "Invalid graph supplied"),
        @ApiResponse(code = 404, message = "Service not found") 
      })
    @Produces("application/ld+json")
    public Response getGroup() {
      
         Client client = Client.create();
         String service = userEndpoint();
         
            WebResource webResource = client.resource(service).queryParam("graph", "urn:csc:groups");

            WebResource.Builder builder = webResource.accept("application/ld+json");

            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatus() != 200) {
               Logger.getLogger(Group.class.getName()).log(Level.INFO, response.getStatus()+" from SERVICE "+service);
               return Response.status(response.getStatus()).entity("{}").build();
            }
            
            ResponseBuilder rb;

                Object context = LDHelper.getGroupContext();        

                Object data;
                try {
                    data = JsonUtils.fromInputStream(response.getEntityInputStream());

                    JsonLdOptions options = new JsonLdOptions();

                    
                    System.out.println(data);
                    Object framed = JsonLdProcessor.frame(data, context, options);

                    rb = Response.status(response.getStatus()); 

                    rb.entity(JsonUtils.toString(framed));
                    
                } catch (JsonLdError ex) {
                    Logger.getLogger(Group.class.getName()).log(Level.SEVERE, null, ex);
                     return Response.serverError().entity("{}").build();
                } catch (IOException ex) {
                    Logger.getLogger(Group.class.getName()).log(Level.SEVERE, null, ex);
                     return Response.serverError().entity("{}").build();
                }
                
           return rb.build();

    }
    
      @DELETE
      @ApiOperation(value = "Delete group", notes = "PUT Body should be json-ld")
      @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Group is removed"),
        @ApiResponse(code = 403, message = "Invalid ID"),
        @ApiResponse(code = 404, message = "Service not found") 
    })
    public Response removeGroup(@ApiParam(value = "Group ID", required = true) @QueryParam("groupID") String groupIDString) {
        
        IRI groupID;      
        try{
           IRIFactory iri = IRIFactory.semanticWebImplementation();
           groupID = iri.construct(groupIDString);
        } catch(IRIException e) {
           Logger.getLogger(User.class.getName()).log(Level.WARNING, "GROUP ID "+groupIDString+" is invalid IRI!");
           return Response.status(403).build();  
        }
        
        String sparqlQuery = "DELETE{ GRAPH <urn:csc:groups> {?group ?p ?o} } WHERE { GRAPH <urn:csc:groups> {?group ?p ?o }}";
        ParameterizedSparqlString pss = new ParameterizedSparqlString(sparqlQuery);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("group", groupID);

        UpdateRequest queryObj = pss.asUpdate(); //UpdateFactory.create(parameterizedSparqlString.as);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(queryObj,userSparqlUpdateEndpoint());
        
        try {
            qexec.execute();
            return Response.status(200).build(); 
        } catch(Exception ex) {
            return Response.status(400).build(); 
        }
 
    }
    
    
}
