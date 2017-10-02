/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Root resource (exposed at "class" path)
 */
@Path("externalClass")
@Api(tags = {"Class"}, description = "External class operations")
public class ExternalClass {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ExternalClass.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get external class from requires", notes = "Get class in JSON-LD")
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
      
        IRI idIRI;   
        
        /* Check that Model URI is valid */
        if(!IDManager.isValidUrl(model)) {
            return JerseyResponseManager.invalidIRI();
        }

      if(id==null || id.equals("undefined") || id.equals("default")) {
          
        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?externalModel rdfs:label ?externalModelLabel . "
                + "?class rdfs:isDefinedBy ?externalModel . "
                + "?class rdfs:label ?label . "
                + "?class rdfs:comment ?comment . "
                + "?class a rdfs:Class . "
                + "?class dcterms:modified ?modified . "
                + "} WHERE { "
                 + "SERVICE ?modelService { "
                 + "GRAPH ?library { "
                 + "?library dcterms:requires ?externalModel . "
                + "?externalModel rdfs:label ?externalModelLabel . "
                 + "}}"
                 + "GRAPH ?externalModel { "
                 + "?class a ?type . "
                 + "VALUES ?type { rdfs:Class owl:Class sh:Shape } "
                /* GET LABEL */
                 + "{?class rdfs:label ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) }"
                 + "UNION"
                 + "{ ?class rdfs:label ?label . FILTER(LANG(?label)!='') }"
                /* GET COMMENT */
                 + "{ ?class ?commentPred ?commentStr . "
                 + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition }"
                 + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
                 + "UNION"
                 + "{ ?class ?commentPred ?comment . "
                 + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition }"
                 + " FILTER(LANG(?comment)!='') }"       
                 + "} "

                 + "}";
        

        pss.setIri("library", model);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
         
        
        pss.setCommandText(queryString);

        
        return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getImportsSparqlAddress());

      } else {
          
            try {
                idIRI = IDManager.constructIRI(id);
            }
            catch (IRIException e) {
                return JerseyResponseManager.invalidIRI();
            }  
              
            String sparqlService = services.getImportsSparqlAddress();

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            /* TODO: FIX dublin core etc. rdf:Property properties */
            
            logger.info("Using ext query:");
            
            String queryString = QueryLibrary.externalClassQuery;
            
            
            logger.info(queryString);
            
            pss.setIri("library", model);
            pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
            pss.setCommandText(queryString);
            pss.setIri("classIRI", idIRI);
            
            
            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }
                        return JerseyJsonLDClient.constructGraphFromService(pss.toString(), sparqlService);         

      }
         
  }

}
