/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
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
    private DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
    private DatasetAdapter adapter = new DatasetAdapter(accessor);
   
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
                @Context HttpServletRequest request) {
      
      
       if(service==null || service.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
        HttpSession session = request.getSession();
        
        if(session==null) return JerseyResponseManager.unauthorized();
        
        LoginSession login = new LoginSession(session);

        if(!(login.isLoggedIn() && (login.getEmail().equals("ytitestaaja@gmail.com") || login.isSuperAdmin()))) {
            return JerseyResponseManager.unauthorized();
        }
        
        Boolean replicate = JerseyJsonLDClient.readBooleanFromURL(service+"replicate");

        if(replicate!=null && replicate.booleanValue()) {
            logger.info("Replicating data from "+service);
        }
        
        IRI modelIRI = null;
       
        try {
                if(model!=null && !model.equals("undefined")) modelIRI = IDManager.constructIRI(model);
        } catch (IRIException e) {
                logger.log(Level.WARNING, "Parameter is invalid IRI!");
               return JerseyResponseManager.invalidIRI();
        }
        
       String SD = "http://www.w3.org/ns/sparql-service-description#";
       Model modelList = JerseyJsonLDClient.getResourceAsJenaModel(service+"serviceDescription");

      if(modelIRI!=null) {
          Resource modelURI = ResourceFactory.createResource(model);
          Resource res = modelList.listSubjectsWithProperty(ResourceFactory.createProperty(SD, "name"), modelURI).nextResource();
          if(res==null) return JerseyResponseManager.invalidParameter();
          importModel(modelURI, service, res);
      } else {
                   
           ResIterator iter = modelList.listResourcesWithProperty(RDF.type, ResourceFactory.createResource(SD+"NamedGraph"));

           while (iter.hasNext()) {

                Resource res = iter.nextResource();
                Resource modelURI = res.getPropertyResourceValue(ResourceFactory.createProperty(SD, "name"));
                importModel(modelURI, service, res);
           
       } 

       }
       

       logger.info("Returning 200 !?!?");
       return JerseyResponseManager.okEmptyContent();

  }


  public void importModel(Resource modelURI, String service, Resource res) {

      if(GraphManager.isExistingGraph(modelURI.toString())) {
          logger.info("Exists! Skipping "+modelURI.toString());
      } else {

          logger.info("---------------------------------------------------------");
          logger.info(modelURI.toString());

                 /* Forget legacy local concepts
                 String localConcepts = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"/skos#",UriComponent.Type.QUERY_PARAM)+"&service=concept";
                 Model localConceptModel = JerseyJsonLDClient.getResourceAsJenaModel(localConcepts);
                 conceptAdapter.add(modelURI.toString()+"/skos#",localConceptModel);
                 Resource modelGROUP = res.getPropertyResourceValue(DCTerms.isPartOf);
                 */

          StmtIterator orgIt = res.listProperties(DCTerms.contributor);

          List<UUID> orgUUIDs = new ArrayList();

          if(orgIt.toList().size()==0) {

              // Fallback organization "YHT ylläpito"
              orgUUIDs.add(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"));

          } else {

              while (orgIt.hasNext()) {
                  orgUUIDs.add(UUID.fromString(orgIt.next().getResource().getLocalName()));
              }

          }

          ServiceDescriptionManager.createGraphDescription(modelURI.toString(), null, orgUUIDs);

          Model exportedModel = JerseyJsonLDClient.getResourceAsJenaModel(service+"exportResource?graph="+modelURI.toString());
          LDHelper.rewriteResourceReference(exportedModel, ResourceFactory.createResource(modelURI.toString()), DCTerms.isPartOf, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/data-theme/GOVE"));


          logger.info("Adding "+modelURI.toString()+" size:"+exportedModel.size());
          adapter.add(modelURI.toString(), exportedModel);


          // HasPartGraph
          String uri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#HasPartGraph",UriComponent.Type.QUERY_PARAM);

          logger.info("HasPartGraph:"+uri);

          Model hasPartModel = JerseyJsonLDClient.getResourceAsJenaModel(uri);
          adapter.add(modelURI.toString()+"#HasPartGraph", hasPartModel);

          // ExportGraph

          String euri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#ExportGraph",UriComponent.Type.QUERY_PARAM);

          logger.info("ExportGraph:"+euri);

          Model exportModel = JerseyJsonLDClient.getResourceAsJenaModel(euri);
          adapter.add(modelURI.toString()+"#ExportGraph", exportModel);

          // PositionGraph

          String puri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#PositionGraph",UriComponent.Type.QUERY_PARAM);

          logger.info("PositionGraph:"+puri);

          Model positionModel = JerseyJsonLDClient.getResourceAsJenaModel(puri);
          adapter.add(modelURI.toString()+"#PositionGraph", positionModel);

          // Resources


          NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);

          while(nodIter.hasNext()) {
              Resource part = nodIter.nextNode().asResource();

              String resourceURI = service+"exportResource?graph="+UriComponent.encode(part.toString(),UriComponent.Type.QUERY_PARAM);

              logger.info("Resource:"+resourceURI);

              Model resourceModel = JerseyJsonLDClient.getResourceAsJenaModel(resourceURI);

              adapter.add(part.toString(), resourceModel);

                    /*
                    NodeIterator subIter = resourceModel.listObjectsOfProperty(DCTerms.subject);


                         while(subIter.hasNext()) {
                             String coConcept = subIter.nextNode().asResource().toString();

                             if(!conceptMap.containsKey(coConcept)) {
                                 String conceptURI = service+"exportResource?graph="+UriComponent.encode(coConcept,UriComponent.Type.QUERY_PARAM)+"&service=concept";
                               //logger.info(conceptURI);
                                 Model conceptModel = JerseyJsonLDClient.getResourceAsJenaModel(conceptURI);
                               //  logger.info(""+conceptModel.size());
                                 conceptMap.put(coConcept, "true");
                                 conceptAdapter.putModel(coConcept, conceptModel);
                             }

                         } */

          }


          logger.info("---------------------------------------------------------");

      }


  }
  

  
}
