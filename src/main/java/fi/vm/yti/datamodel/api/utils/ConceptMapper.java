/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import java.io.InputStream;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

/**
 *
 * @author malonen
 */

/* TODO: REMOVE AFTER TERMED INTEGRATION */
@Deprecated
public class ConceptMapper {
    
     private static EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ConceptMapper.class.getName());
    
     public static Response getFintoIDs() {
         /* Dummy function to load schemes from json */
        ResponseBuilder rb = Response.status(Response.Status.OK);
        
        rb.type("application/ld+json");
        rb.entity(LDHelper.getDefaultSchemes());
       
        return rb.build();
     }
     
     public static Model getModelFromFinto(String id) {
         
           Client client = ClientBuilder.newClient();
            WebTarget target = client.target(services.getVocabExportAPI(id))
                                          .queryParam("format","text/turtle");
            
            Response response = target.request("text/turtle").get();
           

                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    logger.warning("Could not find the vocabulary");
                }
                
                Model model = ModelFactory.createDefaultModel(); 
                
                RDFDataMgr.read(model, response.readEntity(InputStream.class), RDFLanguages.TURTLE);
                
                return model;
     }
      
     public static void updateSchemesFromFinto() {
         
         Response resp = getFintoIDs();
         Model model = JerseyClient.getJSONLDResponseAsJenaModel(resp);
         Model schemeModel = ModelFactory.createDefaultModel();
         schemeModel.setNsPrefixes(LDHelper.PREFIX_MAP);
         
         Property vID = ResourceFactory.createProperty("http://schema.onki.fi/onki#vocabularyIdentifier");
         NodeIterator idIter = model.listObjectsOfProperty(vID);
         DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getTempConceptReadWriteAddress());
         
         while(idIter.hasNext()) {
             String fintoID = idIter.next().asLiteral().toString();
           if(!accessor.containsModel(ApplicationProperties.getSchemeId()+fintoID)) {
              logger.info("Loading "+fintoID+" from "+ApplicationProperties.getSchemeId());
              Model vocabModel = getModelFromFinto(fintoID);
              
              ResIterator resIter = vocabModel.listResourcesWithProperty(RDF.type, SKOS.ConceptScheme);
              
              while(resIter.hasNext()) {
                  Resource schemeResource = resIter.next();
                  schemeResource.addLiteral(DC.identifier, fintoID);
                  schemeResource.addProperty(DCTerms.isFormatOf, ResourceFactory.createResource(ApplicationProperties.getSchemeId()+fintoID));
                          
                  accessor.putModel(ApplicationProperties.getSchemeId()+fintoID, vocabModel);
                  
                  StmtIterator titles = schemeResource.listProperties(DC.title);
                  StmtIterator desc = schemeResource.listProperties(DC.description);
                  
                  schemeModel.add(desc);
                  schemeModel.add(titles);
                  schemeModel.add(schemeResource, RDF.type, SKOS.ConceptScheme);
                  schemeModel.add(schemeResource, DC.identifier, fintoID);
                  schemeModel.add(schemeResource, DCTerms.isFormatOf, ResourceFactory.createResource(ApplicationProperties.getSchemeId()+fintoID));
              }
             
           }
             
           if(schemeModel.size()>1) {
                accessor.add("urn:csc:schemes",schemeModel);
           }
         }
         
         
     }
     
     /*
      public static void updateConceptFromConceptService(String uri) {
        
        if(!uri.startsWith("urn:uuid:")) {   
            
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getTempConceptReadWriteAddress());
            
            if(!accessor.containsModel(uri)) {
         
                Client client = Client.create();

                WebResource webResource = client.resource(services.getConceptAPI())
                                          .queryParam("uri", UriComponent.encode(uri,UriComponent.Type.QUERY))
                                          .queryParam("format","application/json");

                WebResource.Builder builder = webResource.accept("application/json");
                ClientResponse response = builder.get(ClientResponse.class);

                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    logger.warning("Could not find the concept");
                }

                Model model = ModelFactory.createDefaultModel(); 

                RDFDataMgr.read(model, response.getEntityInputStream(), RDFLanguages.JSONLD);
                
                accessor.add(uri, model);

                logger.info("Updated "+uri+" from "+services.getConceptAPI());
                
            } else {
                logger.info("Concept already exists!");
            }
        } 

  
    }    
    */

    @Deprecated
    public static void addConceptFromReferencedResource(String model, String classID) {
                /*
        String query
                = "INSERT { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + "WHERE { "
                + "SERVICE ?modelService {"
                + "GRAPH ?class {"
                + "?class dcterms:subject ?concept . "
                + "}}"
                + "GRAPH ?vocabulary {"
                + "?concept a skos:Concept . }"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("class", classID);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setCommandText(query);
        
        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute();
        */
        
    }

    @Deprecated
    public static void removeUnusedConcepts(String model) {
        /*
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT DISTINCT ?subject "
                + "WHERE { "
                + "{GRAPH ?skosCollection { ?skosCollection skos:member ?subject . }} "
                + " MINUS "
                + "{SERVICE ?modelService {"
                + "GRAPH ?hasPartGraph { "
                + " ?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource rdfs:isDefinedBy ?model . "
                + "?resource dcterms:subject ?subject . "
                + "}}}"
                + "}";

        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setIri("model", model);
        pss.setIri("hasPartGraph", model+"#HasPartGraph");
        pss.setIri("skosCollection", model+"/skos#");
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getTempConceptReadSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            Resource resSubject = soln.getResource("subject");
            if(resSubject!=null) {
            String subject = soln.getResource("subject").toString();
            logger.info("Unused concept: "+subject);
            deleteConceptReference(model,subject);
            
            if(!isUsedConceptGlobal(subject)) adapter.deleteModel(subject);
            
            }
        }
*/
    }
    /*
    public static void resolveConcept(String resource) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources = "SELECT ?subject WHERE { GRAPH ?resource { ?resource dcterms:subject ?subject . }}";

        pss.setIri("resource", resource);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.asQuery());

        ResultSet results = qexec.execSelect();

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
             Resource resSubject = soln.getResource("subject");
            if(resSubject!=null) {
                String subject = resSubject.toString();
                ConceptMapper.updateConceptFromConceptService(subject);
            }
        }
    }
*/
    @Deprecated
    public static void addConceptToLocalSKOSCollection(String model, String concept) {
        /*
        String query
                = " INSERT { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + " WHERE { ?concept a skos:concept . }";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("concept", concept);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute(); */

    }

    @Deprecated
        public static boolean deleteModelReference(String model, String concept) {
            /*
            if(isUsedConcept(model, concept)) {
               return false;
            }
            else {
             deleteConceptReference(model, concept);  
             return true;
            }*/
            return true;
        }
        

    @Deprecated
    public static void deleteConceptReference(String model, String concept) {
        
          /*
        String query
                = " DELETE { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}"
                + " WHERE { GRAPH ?skosCollection { ?skosCollection skos:member ?concept . }}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("skosCollection", model+"/skos#");
        pss.setIri("concept", concept);
        pss.setCommandText(query);
        

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getTempConceptSparqlUpdateAddress());
        qexec.execute(); */

    }

    @Deprecated
    public static void deleteConceptSuggestion(String model, String concept) {
        /*
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getTempConceptReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        
        adapter.deleteModel(concept);
                   
       deleteConceptReference(model, concept); */

    }

    @Deprecated
    public static void updateConceptSuggestion(String conceptID) {
        /* dcterms:subject ?concept . ?concept skos:definition ?oldDefinition . }}"
                + " INSERT { GRAPH ?graph { ?graph dcterms:subject ?concept . ?concept skos:definition ?definition . }}"
                + " WHERE { "
                + "GRAPH ?graph { ?graph dcterms:subject ?concept . ?concept skos:definition ?oldDefinition . }"
                + "SERVICE ?conceptService { GRAPH ?concept { ?concept skos:definition ?definition . }}"
                + "}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("conceptService", services.getLocalhostConceptSparqlAddress());
        pss.setIri("concept", conceptID);
        pss.setCommandText(query);

        UpdateRequest queryObj = pss.asUpdate();
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, services.getCoreSparqlUpdateAddress());
        qexec.execute(); */
    }

    
}
