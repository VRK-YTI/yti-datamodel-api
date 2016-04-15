/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Root resource (exposed at "class" path)
 */
@Path("externalPredicate")
@Api(value = "/externalPredicate", description = "External predicate operations")
public class ExternalPredicate {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ExternalPredicate.class.getName());
    
  @GET
  @Produces("application/ld+json")
  @ApiOperation(value = "Get external predicate from required model", notes = "Get predicate in JSON-LD")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "No such resource"),
      @ApiResponse(code = 400, message = "Invalid model supplied"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Internal server error")
  })
  public Response json(
      @ApiParam(value = "Predicate id")
      @QueryParam("id") String id,
      @ApiParam(value = "Model id")
      @QueryParam("model") String model) {
      
      
        IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
        IRI modelIRI, idIRI;   
        
        /* Check that Model URI is valid */
        try {
            modelIRI = iriFactory.construct(model);
        }
        catch(NullPointerException e) {
            return Response.status(403).entity(ErrorMessage.UNEXPECTED).build();
        }
        catch (IRIException e) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }

      if(id==null || id.equals("undefined") || id.equals("default")) {
          
        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?externalModel rdfs:label ?externalModelLabel . "
                + "?predicate rdfs:isDefinedBy ?externalModel . "
                + "?predicate rdfs:label ?label . "
                + "?predicate a ?type . "
                + "?predicate dcterms:modified ?modified . "
                + "?predicate rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabel . "
                + "} WHERE { "
                 + "SERVICE ?modelService { "
                     + "GRAPH ?library { "
                     + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                     + "}}"
                 + "GRAPH ?externalModel { "
                 + "?predicate a ?type . "
                 + "VALUES ?type { owl:DatatypeProperty owl:ObjectProperty } "
                 + "?predicate rdfs:label ?labelStr . BIND(STRLANG(?labelStr,'en') as ?label) "
              /*   + "OPTIONAL { ?predicate rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabelStr .  "
                + "BIND(STRLANG(?sourceLabelStr,'en') as ?sourceLabel)} " */
                 + "} "
                 + "}";
        
        pss.setIri("library", model);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
         
        pss.setCommandText(queryString);

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getImportsSparqlAddress());

      } else {
          
            try {
                idIRI = iriFactory.construct(id);
            }
            catch (IRIException e) {
                return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }  
              
            String sparqlService = services.getImportsSparqlAddress();

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
     
           String queryString = "CONSTRUCT { "
                + "?externalModel rdfs:label ?externalModelLabel . "
                + "?predicate rdfs:isDefinedBy ?externalModel . "
                + "?predicate rdfs:label ?label . "
                + "?predicate rdfs:comment ?comment . "
                + "?predicate a ?type . "
                + "?predicate dcterms:modified ?modified . "
                + "?predicate rdfs:range ?range . "
                + "?predicate rdfs:domain ?domain . "
                + "?predicate rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabel . "
                + "} WHERE { "
                 + "SERVICE ?modelService { "
                     + "GRAPH ?library { "
                     + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                     + "}}"
                 + "GRAPH ?externalModel { "
                 + "?predicate a ?type . "
                 + "VALUES ?type { owl:DatatypeProperty owl:ObjectProperty rdf:Property } "
                 + "OPTIONAL { ?predicate rdfs:range ?range . }"
                 + "OPTIONAL { ?predicate rdfs:domain ?domain . }"
                 + "?predicate rdfs:label ?labelStr . BIND(STRLANG(?labelStr,'en') as ?label) "
                 + "OPTIONAL { ?predicate ?commentPred ?commentStr . "
                 + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                 + "BIND(STRLANG(?commentStr,'en') as ?comment) "
                 + "}"
                 + "OPTIONAL { ?predicate rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabelStr .  "
                + "BIND(STRLANG(?sourceLabelStr,'en') as ?sourceLabel)} "
                 + "} "
                 + "}";
        
            pss.setCommandText(queryString);

            pss.setIri("predicate", idIRI);   
            pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
         

            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }
                        return JerseyFusekiClient.constructGraphFromService(pss.toString(), sparqlService);         

      }
         
  }

}
