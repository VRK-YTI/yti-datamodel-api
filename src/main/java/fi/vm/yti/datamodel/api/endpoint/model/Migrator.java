/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.glassfish.jersey.uri.UriComponent;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("migrate")
@Api(tags = {"Admin"}, description = "Migrates datamodels from iow.csc.fi")
public class Migrator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Migrator.class.getName());
    private DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
    private DatasetAdapter adapter = new DatasetAdapter(accessor);

    @GET
    @ApiOperation(value = "OK to migrate")
    @Produces("application/json")
    public boolean getStatus(@Context HttpServletRequest request) {
      /* Add proper versioning logic */
      return true;
    }

    /**
     * Migrate from given service address
     * @returns empty Response
     */
  @PUT
  @ApiOperation(value = "Migrates graph from old iow.csc.fi instance and writes service description to default", notes = "PUT Body should be json-ld")
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
       logger.info("Service description: "+service+"serviceDescription size:"+modelList.size());

      if(modelIRI!=null) {
          Resource modelURI = ResourceFactory.createResource(model);
          ResIterator rit = modelList.listSubjectsWithProperty(ResourceFactory.createProperty(SD, "name"), modelURI);
          if(rit.hasNext()) {
              Resource res = rit.nextResource();
              migrateModel(modelURI, service, res);
          } else {
              return JerseyResponseManager.invalidParameter();
          }
      } else {
           return JerseyResponseManager.invalidParameter();
       }

       logger.info("Returning 200 !?!?");
       return JerseyResponseManager.okEmptyContent();

  }


  public void migrateModel(Resource modelURI, String service, Resource res) {

      String oldNamespace = modelURI.toString();
      String oldNamespaceDomain = oldNamespace.substring(0,oldNamespace.lastIndexOf("/")+1);
      logger.info("Old namespace domain: "+oldNamespaceDomain);

      Model exportedModel = JerseyJsonLDClient.getResourceAsJenaModel(service+"exportResource?graph="+oldNamespace);
      String prefix = exportedModel.listStatements(ResourceFactory.createResource(oldNamespace), LDHelper.curieToProperty("dcap:preferredXMLNamespacePrefix"), (Literal) null).nextStatement().getString();
      logger.info("Model prefix: "+prefix);

      String namespaceDomain = "http://uri.suomi.fi/datamodel/ns/";
      String newNamespace = namespaceDomain+prefix;

      if(GraphManager.isExistingGraph(newNamespace)) {
          logger.info("Exists! Skipping "+modelURI.toString());
      } else {

          logger.info("---------------------------------------------------------");


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

          LDHelper.rewriteLiteral(exportedModel, ResourceFactory.createResource(oldNamespace), LDHelper.curieToProperty("dcap:preferredXMLNamespaceName"), ResourceFactory.createPlainLiteral(newNamespace+"#"));

          String deleteGroup =
                  "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
                          "PREFIX dcterms: <http://purl.org/dc/terms/>" +
                          "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
                          "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                          "PREFIX termed: <http://termed.thl.fi/meta/>" +
                          "DELETE {" +
                          "    ?model dcterms:isPartOf ?group . " +
                          "    ?group ?p ?o . " +
                          "} INSERT { " +
                          "    ?model dcterms:isPartOf <http://publications.europa.eu/resource/authority/data-theme/GOVE> ."+
                          "} WHERE { " +
                          "    ?model a owl:Ontology . " +
                          "    ?model dcterms:isPartOf ?group . " +
                          "    ?group ?p ?o . " +
                          "}";

          UpdateAction.parseExecute(deleteGroup, exportedModel);
        //  LDHelper.rewriteResourceReference(exportedModel, ResourceFactory.createResource(modelURI.toString()), DCTerms.isPartOf, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/data-theme/GOVE"));


          String deleteTerminology =
                          "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
                          "PREFIX dcterms: <http://purl.org/dc/terms/>" +
                          "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
                          "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                          "PREFIX termed: <http://termed.thl.fi/meta/>" +
                          "DELETE {" +
                          "    ?model dcterms:references ?collection . " +
                          "    ?collection ?p ?o . " +
                          "} WHERE { " +
                          "    ?model a owl:Ontology . " +
                          "    ?model dcterms:references ?collection . " +
                          "    ?collection dcterms:identifier ?any . " +
                          "    ?collection ?p ?o . " +
                          "}";


          UpdateAction.parseExecute(deleteTerminology, exportedModel);



          String deleteRequired =
                  "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
                          "PREFIX dcterms: <http://purl.org/dc/terms/>" +
                          "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
                          "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                          "PREFIX termed: <http://termed.thl.fi/meta/>" +
                          "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                          "PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>" +
                          "DELETE {" +
                          "    ?model dcterms:requires ?collection . " +
                          "    ?collection ?p ?o . " +
                          "} WHERE { " +
                          "    ?model a owl:Ontology . " +
                          "    ?model dcterms:requires ?collection . " +
                          "    FILTER NOT EXISTS { ?collection a rdfs:Resource . }" +
                          "    ?collection ?p ?o . " +
                          "}";


          UpdateAction.parseExecute(deleteRequired, exportedModel);

          String updateStatus =
                  "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
                          "PREFIX dcterms: <http://purl.org/dc/terms/>" +
                          "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
                          "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                          "PREFIX termed: <http://termed.thl.fi/meta/>" +
                          "DELETE {" +
                          "    ?resource owl:versionInfo ?any . " +
                          "}"+
                          "INSERT {" +
                          "    ?resource owl:versionInfo 'DRAFT' . "+
                          "} " +
                          "WHERE { " +
                          "    ?resource owl:versionInfo ?any . " +
                          "}";

          UpdateAction.parseExecute(updateStatus, exportedModel);


          logger.info("Migrating "+oldNamespace+" size:"+exportedModel.size()+" to "+newNamespace);
          exportedModel = NamespaceManager.renameNamespace(exportedModel, oldNamespaceDomain, namespaceDomain);

          //exportedModel.write(System.out, "text/turtle");

          adapter.putModel(newNamespace, exportedModel);


          ServiceDescriptionManager.createGraphDescription(newNamespace, null, orgUUIDs);

          // HasPartGraph
          String uri = service+"exportResource?graph="+UriComponent.encode(oldNamespace+"#HasPartGraph",UriComponent.Type.QUERY_PARAM);

          logger.info("Getting HasPartGraph: "+uri);

          Model hasPartModel = JerseyJsonLDClient.getResourceAsJenaModel(uri);

          // ExportGraph

          String euri = service+"exportResource?graph="+UriComponent.encode(oldNamespace+"#ExportGraph",UriComponent.Type.QUERY_PARAM);
          logger.info("ExportGraph:"+euri);

          Model exportModel = JerseyJsonLDClient.getResourceAsJenaModel(euri);

          exportModel = NamespaceManager.renameNamespace(exportModel, oldNamespaceDomain, namespaceDomain);
          adapter.putModel(newNamespace+"#ExportGraph", exportModel);

          // PositionGraph

          String puri = service+"exportResource?graph="+UriComponent.encode(oldNamespace+"#PositionGraph",UriComponent.Type.QUERY_PARAM);

          logger.info("PositionGraph:"+puri);

          Model positionModel = JerseyJsonLDClient.getResourceAsJenaModel(puri);

          if(positionModel!=null && positionModel.size()>1) {
              positionModel = NamespaceManager.renameNamespace(positionModel, oldNamespaceDomain, namespaceDomain);
              adapter.putModel(newNamespace + "#PositionGraph", positionModel);
          }

          // Resources

          NodeIterator nodIter = hasPartModel.listObjectsOfProperty(DCTerms.hasPart);


          while(nodIter.hasNext()) {
              Resource part = nodIter.nextNode().asResource();
              String oldName = part.toString();

              if (oldName.startsWith(oldNamespace)) {

                  String resourceURI = service + "exportResource?graph=" + UriComponent.encode(oldName, UriComponent.Type.QUERY_PARAM);

                  Model resourceModel = JerseyJsonLDClient.getResourceAsJenaModel(resourceURI);
                  String newName = oldName.replaceFirst(oldNamespace, newNamespace);

                  resourceModel = NamespaceManager.renameNamespace(resourceModel, oldNamespaceDomain, namespaceDomain);

                  // resourceModel.write(System.out, "text/turtle");

                  String deleteLocalSkos =
                          "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
                                  "PREFIX dcterms: <http://purl.org/dc/terms/>" +
                                  "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
                                  "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                                  "PREFIX termed: <http://termed.thl.fi/meta/>" +
                                  "INSERT {" +
                                  "    ?collection dcterms:title 'Sisäinen käsitteistö'@fi . " +
                                  "} WHERE { " +
                                  "    ?resource skos:inScheme ?collection . " +
                                  "    FILTER NOT EXISTS{?collection dcterms:title ?o . }" +
                                  "}";

                  UpdateAction.parseExecute(deleteLocalSkos, resourceModel);

                  UpdateAction.parseExecute(updateStatus, resourceModel);

                  adapter.putModel(newName, resourceModel);

                  logger.info("Migrated resource " + oldName + " to " + newName);
              } else {
                  logger.warning("Reference to external resource " + oldName);
              }
          }

          // Creating new HasPartGraph
          hasPartModel = NamespaceManager.renameNamespace(hasPartModel, oldNamespaceDomain, namespaceDomain);
          hasPartModel.write(System.out,"text/turtle");
          adapter.putModel(newNamespace+"#HasPartGraph", hasPartModel);

          // GraphManager.constructExportGraph(newNamespace);


          logger.info("---------------------------------------------------------");

      }


  }
  

  
}
