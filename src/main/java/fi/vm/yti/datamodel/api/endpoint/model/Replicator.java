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
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
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
                @Context HttpServletRequest request) {
      
      
       if(service==null || service.equals("undefined")) {
            return JerseyResponseManager.invalidIRI();
       } 
       
        HttpSession session = request.getSession();

        LoginSession login = new LoginSession(session);

        if(!login.isSuperAdmin()) {
            return JerseyResponseManager.unauthorized();
        }
        
        Boolean replicate = JerseyJsonLDClient.readBooleanFromURL(service+"replicate");

        if(replicate!=null && replicate.booleanValue()) {
            logger.info("Replicating data from "+service);
        }

      try {

          Model modelList = JerseyJsonLDClient.getResourceAsJenaModel(service + "serviceDescription");
          logger.info("Service description size: " + modelList.size());

          replicateServices(service, login);

          return JerseyResponseManager.okEmptyContent();

      } catch(Exception ex) {
            logger.warning(ex.getStackTrace().toString());
            return JerseyResponseManager.error();
      }

  }

  @Deprecated
  public void importModel(Resource modelURI, String service, Resource res, LoginSession login) {

      if (GraphManager.isExistingGraph(modelURI.toString())) {
          logger.info("Exists! Skipping " + modelURI.toString());
      } else {

          logger.info("---------------------------------------------------------");
          logger.info(modelURI.toString());

          StmtIterator orgIt = res.listProperties(DCTerms.contributor);

          List<UUID> orgUUIDs = new ArrayList();

          if (orgIt.toList().size() == 0) {

              // Fallback organization "YHT ylläpito"
              orgUUIDs.add(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"));

          } else {

              while (orgIt.hasNext()) {
                  orgUUIDs.add(UUID.fromString(orgIt.next().getResource().getLocalName()));
              }

          }

          ServiceDescriptionManager.createGraphDescription(modelURI.toString(), null, orgUUIDs);

          Model exportedModel = JerseyJsonLDClient.getResourceAsJenaModel(service + "exportResource?graph=" + modelURI.toString());

          logger.info("Adding " + modelURI.toString() + " size:" + exportedModel.size());
          adapter.putModel(modelURI.toString(), exportedModel);

          /*
          Model historyModel = JerseyJsonLDClient.getResourceAsJenaModel(service+"exportResource?id="+modelURI.toString());
          NodeIterator nodIter = historyModel.listObjectsOfProperty(DCTerms.hasPart);

          if(ProvenanceManager.getProvMode()) {
              ProvenanceManager.createProvenanceGraphFromModel(imported.getId(), imported.asGraph(), null, importedModel.getProvUUID());
              ProvenanceManager.createProvEntity(importedModel.getId(), login. , newVocabulary.getProvUUID());
          }*/

          // HasPartGraph
          String uri = service + "exportResource?graph=" + UriComponent.encode(modelURI.toString() + "#HasPartGraph", UriComponent.Type.QUERY_PARAM);

          logger.info("HasPartGraph:" + uri);

          Model hasPartModel = JerseyJsonLDClient.getResourceAsJenaModel(uri);
          adapter.putModel(modelURI.toString() + "#HasPartGraph", hasPartModel);

          // ExportGraph

          String euri = service + "exportResource?graph=" + UriComponent.encode(modelURI.toString() + "#ExportGraph", UriComponent.Type.QUERY_PARAM);

          logger.info("ExportGraph:" + euri);

          Model exportModel = JerseyJsonLDClient.getResourceAsJenaModel(euri);
          adapter.putModel(modelURI.toString() + "#ExportGraph", exportModel);

          // PositionGraph

          String puri = service + "exportResource?graph=" + UriComponent.encode(modelURI.toString() + "#PositionGraph", UriComponent.Type.QUERY_PARAM);

          logger.info("PositionGraph:" + puri);

          Model positionModel = JerseyJsonLDClient.getResourceAsJenaModel(puri);
          adapter.putModel(modelURI.toString() + "#PositionGraph", positionModel);

          // Resources

          NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);

          while (nodIter.hasNext()) {
              Resource part = nodIter.nextNode().asResource();

              String resourceURI = service + "exportResource?graph=" + UriComponent.encode(part.toString(), UriComponent.Type.QUERY_PARAM);

              logger.info("Resource:" + resourceURI);

              Model resourceModel = JerseyJsonLDClient.getResourceAsJenaModel(resourceURI);


              adapter.putModel(part.toString(), resourceModel);


          }


          logger.info("---------------------------------------------------------");

      }
  }

  @Deprecated
  public void replicateModels(Model serviceDescription, String service, LoginSession login) {

        logger.warning("Dropping everything and replicating "+service);

        GraphManager.deleteGraphs();
        GraphManager.createDefaultGraph();
        RHPOrganizationManager.initOrganizationsFromRHP();
        GraphManager.initServiceCategories();
        NamespaceManager.addDefaultNamespacesToCore();

        // Copy whole service description from other service
        logger.info("Service description size: "+serviceDescription.size());
        adapter.putModel("urn:csc:iow:sd", serviceDescription);

        serviceDescription.write(System.out, "text/turtle");

        ResIterator iter = serviceDescription.listResourcesWithProperty(RDF.type, ServiceDescriptionManager.NamedGraph);

        while (iter.hasNext()) {

            Resource res = iter.nextResource();
            Resource modelURI = res.getPropertyResourceValue(ServiceDescriptionManager.name);

            logger.info("Trying to import model "+modelURI.toString());

            if(GraphManager.isExistingGraph(modelURI.toString())) {
                logger.info("Exists! Skipping "+modelURI.toString());
            } else {

                logger.info("---------------------------------------------------------");
                logger.info(modelURI.toString());

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

                Model exportedModel = JerseyJsonLDClient.getResourceAsJenaModel(service+"exportResource?graph="+modelURI.toString());

                logger.info("Adding "+modelURI.toString()+" size:"+exportedModel.size());
                adapter.putModel(modelURI.toString(), exportedModel);


              Model historyModel = JerseyJsonLDClient.getResourceAsJenaModel(service+"history?id="+modelURI.toString());
              ProvenanceManager.putToProvenanceGraph(historyModel, modelURI.toString());
              NodeIterator entityIter = historyModel.listObjectsOfProperty(ProvenanceManager.generatedAtTime);

              while(entityIter.hasNext()) {
                  String provResURI = entityIter.next().asResource().toString();
                  Model provResource = JerseyJsonLDClient.getResourceAsJenaModel(service+"history?id="+provResURI);
                  ProvenanceManager.putToProvenanceGraph(provResource, provResURI);
              }

                // HasPartGraph
                String uri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#HasPartGraph",UriComponent.Type.QUERY_PARAM);

                logger.info("HasPartGraph:"+uri);

                Model hasPartModel = JerseyJsonLDClient.getResourceAsJenaModel(uri);
                adapter.putModel(modelURI.toString()+"#HasPartGraph", hasPartModel);

                // ExportGraph

                String euri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#ExportGraph",UriComponent.Type.QUERY_PARAM);

                logger.info("ExportGraph:"+euri);

                Model exportModel = JerseyJsonLDClient.getResourceAsJenaModel(euri);
                adapter.putModel(modelURI.toString()+"#ExportGraph", exportModel);

                // PositionGraph

                String puri = service+"exportResource?graph="+UriComponent.encode(modelURI.toString()+"#PositionGraph",UriComponent.Type.QUERY_PARAM);

                logger.info("PositionGraph:"+puri);

                Model positionModel = JerseyJsonLDClient.getResourceAsJenaModel(puri);
                adapter.putModel(modelURI.toString()+"#PositionGraph", positionModel);

                // Resources

                NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);

                while(nodIter.hasNext()) {
                    Resource part = nodIter.nextNode().asResource();

                    String resourceURL = service+"exportResource?graph="+UriComponent.encode(part.toString(),UriComponent.Type.QUERY_PARAM);

                    logger.info("Resource:"+resourceURL);

                    Model resourceModel = JerseyJsonLDClient.getResourceAsJenaModel(resourceURL);
                    resourceModel.write(System.out, "text/turtle");

                    adapter.putModel(part.toString(), resourceModel);

                    String historyURL = service+"history?id="+part.toString();
                    logger.info("History activity:"+historyURL);

                    Model resourceHistoryModel = JerseyJsonLDClient.getResourceAsJenaModel(historyURL);
                    resourceHistoryModel.write(System.out,"text/turtle");

                    ProvenanceManager.putToProvenanceGraph(resourceHistoryModel, part.toString());
                    NodeIterator resProvIter = historyModel.listObjectsOfProperty(ProvenanceManager.generatedAtTime);

                    while(resProvIter.hasNext()) {
                        String provResURI = resProvIter.next().asResource().toString();
                        logger.info("Replicating "+part.toString()+" history "+provResURI);
                        Model provRes = JerseyJsonLDClient.getResourceAsJenaModel(service+"history?id="+provResURI);
                        provRes.write(System.out,"text/turtle");
                        ProvenanceManager.putToProvenanceGraph(provRes, provResURI);
                    }

                }

            }


            logger.info("---------------------------------------------------------");

        }



  }

  public void replicateServices(String externalService, LoginSession login) {

      GraphManager.deleteGraphs();

      try (RDFConnection conn = RDFConnectionFactory.connect(services.getEndpoint()+"/core") ) {
          Txn.executeWrite(conn, ()-> {
              Dataset externalDataset = JerseyJsonLDClient.getExternalTRIGDataset(externalService + "exportGraphs?service=core&content-type=text/trig");
              logger.info("Size of the CORE dataset: "+externalDataset.asDatasetGraph().size());
              conn.loadDataset(externalDataset);
          });
      }

      try (RDFConnection conn = RDFConnectionFactory.connect(services.getEndpoint()+"/prov") ) {
          Txn.executeWrite(conn, ()-> {
              Dataset externalDataset = JerseyJsonLDClient.getExternalTRIGDataset(externalService + "exportGraphs?service=prov&content-type=text/trig");
              logger.info("Size of the PROV dataset: "+externalDataset.asDatasetGraph().size());
              conn.loadDataset(externalDataset);
          });
      }

  }

  
}
