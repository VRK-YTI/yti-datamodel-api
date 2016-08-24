/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.DataInputStream;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("replicate")
@Api(value = "/replicate", description = "Returns information about replicable models")
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
                @ApiParam(value = "Model ID") 
                @QueryParam("model") 
                String model,
                @ApiParam(value = "Group ID") 
                @QueryParam("group") 
                String group,
                @Context HttpServletRequest request) {
      
      
       if(service==null || service.equals("undefined")) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
       } 
       
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!(login.isLoggedIn() && login.isSuperAdmin())) {
            return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
        }
        
        Boolean replicate;
        
        try {
            
            String replicateURI = service+"replicate";
            
            Client client = Client.create();
            WebResource webResource = client.resource(replicateURI);
            WebResource.Builder builder = webResource.accept("application/json");
            ClientResponse response = builder.get(ClientResponse.class);

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info(service);
                logger.info(""+response.getStatus());
                  return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }

            DataInputStream dis = new DataInputStream(response.getEntityInputStream());
            replicate = new Boolean(dis.readBoolean());
        
        } catch(Exception ex) {
            return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
        }
        
        if(replicate!=null && replicate.booleanValue()) {
            logger.info("Replicating data from "+replicate);
        }
        
        IRI modelIRI = null, groupIRI = null;
       
        try {
                IRIFactory iri = IRIFactory.iriImplementation();
                if(model!=null && !model.equals("undefined")) modelIRI = iri.construct(model);
                if(group!=null && !group.equals("undefined")) groupIRI = iri.construct(group);
        } catch (IRIException e) {
                logger.log(Level.WARNING, "Parameter is invalid IRI!");
               return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
        
       String SD = "http://www.w3.org/ns/sparql-service-description#";
         
       if(modelIRI==null && groupIRI==null) {
           
           Model modelList = JerseyFusekiClient.getResourceAsJenaModel(service+"serviceDescription");
           ResIterator iter = modelList.listResourcesWithProperty(RDF.type, ResourceFactory.createResource(SD+"NamedGraph"));
           
           DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
           DatasetAdapter adapter = new DatasetAdapter(accessor);
           
           DatasetGraphAccessorHTTP conceptAccessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
           DatasetAdapter conceptAdapter = new DatasetAdapter(conceptAccessor);
           
           
           while (iter.hasNext()) {
                Resource res = iter.nextResource();
                Resource modelURI = res.getPropertyResourceValue(ResourceFactory.createProperty(SD, "name"));
                
                if(GraphManager.isExistingGraph(modelURI.toString())) {
                    logger.info("Exists! Skipping "+modelURI.toString());
                } else {
              
                 logger.info("---------------------------------------------------------");    
                 logger.info(modelURI.toString());
                 
                /* Replicate local concepts */ 
                String localConcepts = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"/skos#",UriComponent.Type.QUERY_PARAM)+"&service=concept";
                Model localConceptModel = JerseyFusekiClient.getResourceAsJenaModel(localConcepts);
                conceptAdapter.add(modelURI.toString()+"/skos#",localConceptModel);
                 
                Resource modelGROUP = res.getPropertyResourceValue(DCTerms.isPartOf);
                
                
                ServiceDescriptionManager.createGraphDescription(modelURI.toString(), modelGROUP.toString(), null);
                
                Model exportedModel = JerseyFusekiClient.getResourceAsJenaModel(service+"exportResource?graph="+modelURI.toString());
                adapter.add(modelURI.toString(), exportedModel);
                
                // HasPartGraph
                String uri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#HasPartGraph",UriComponent.Type.QUERY_PARAM);
     
                Model hasPartModel = JerseyFusekiClient.getResourceAsJenaModel(uri);
                adapter.add(modelURI.toString()+"#HasPartGraph", hasPartModel);
                
                // ExportGraph
                
                String euri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#ExportGraph",UriComponent.Type.QUERY_PARAM);
     
                Model exportModel = JerseyFusekiClient.getResourceAsJenaModel(euri);
                adapter.add(modelURI.toString()+"#ExportGraph", exportModel);
                
                // PositionGraph
                
                String puri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#PositionGraph",UriComponent.Type.QUERY_PARAM);
     
                Model positionModel = JerseyFusekiClient.getResourceAsJenaModel(puri);
                adapter.add(modelURI.toString()+"#PositionGraph", positionModel);
                
                
                // Resources
                
                NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);
                HashMap conceptMap = new HashMap<String, String>();
                    
                while(nodIter.hasNext()) {
                    Resource part = nodIter.nextNode().asResource();
                    
                    String resourceURI = service+"exportResource?graph="+UriComponent.encode(part.toString(),UriComponent.Type.QUERY_PARAM);
                    Model resourceModel = JerseyFusekiClient.getResourceAsJenaModel(resourceURI);
                    adapter.add(part.toString(), resourceModel);
                    
                    NodeIterator subIter = resourceModel.listObjectsOfProperty(DCTerms.subject);
                   
                    while(subIter.hasNext()) {
                        String coConcept = subIter.nextNode().asResource().toString();
                        
                        if(!conceptMap.containsKey(coConcept)) {
                            String conceptURI = service+"exportResource?graph="+UriComponent.encode(coConcept,UriComponent.Type.QUERY_PARAM)+"&service=concept";
                          //logger.info(conceptURI);
                            Model conceptModel = JerseyFusekiClient.getResourceAsJenaModel(conceptURI);
                          //  logger.info(""+conceptModel.size());
                            conceptMap.put(coConcept, "true");
                            conceptAdapter.putModel(coConcept, conceptModel);
                        }
                        
                        
                    }
                        
                    
                }
                
           }
           
       } 
           
       logger.info("---------------------------------------------------------");
       }
       

       logger.info("Returning 200 !?!?");
       return Response.status(200).entity("{}").build();

  }
  

  
}
