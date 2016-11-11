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
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
      
      
        IRIFactory iriFactory = IRIFactory.iriImplementation();
        IRI modelIRI, idIRI;   
        
        /* Check that Model URI is valid */
        try {
            modelIRI = iriFactory.construct(model);
        }
        catch(NullPointerException e) {
            return JerseyResponseManager.unexpected();
        }
        catch (IRIException e) {
            return JerseyResponseManager.invalidIRI();
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
                /* IF Predicate type is known */
                + "{"
                + "?predicate a owl:DatatypeProperty . "
                + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
                + "BIND(owl:DatatypeProperty as ?type) "
                + "} UNION {"
                + "?predicate a owl:ObjectProperty . "
                + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                + "BIND(owl:ObjectProperty as ?type) "
                + "} UNION {"
                /* Treat owl:AnnotationProperty as DatatypeProperty */
                + "?predicate a owl:AnnotationProperty. "
                + "?predicate rdfs:label ?atLeastSomeLabel . "
                + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                + "BIND(owl:DatatypeProperty as ?type) "
                + "} UNION {"
                /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
                + "?predicate a rdf:Property . "
                + "?predicate rdfs:range rdfs:Literal ."
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                + "BIND(owl:DatatypeProperty as ?type) "
                 + "} UNION {"
                /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
                + "?predicate a rdf:Property . "
                + "?predicate rdfs:range rdfs:Resource ."
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                + "BIND(owl:ObjectProperty as ?type) "
                + "}UNION {"
                /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
                + "?predicate a rdf:Property . "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                + "?predicate rdfs:range ?rangeClass . "
                + "FILTER(?rangeClass!=rdfs:Literal)"
                + "?rangeClass a ?rangeClassType . "
                + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
                + "BIND(owl:ObjectProperty as ?type) "
                + "} UNION {"
                /* IF Predicate type cannot be guessed */
                + "?predicate a rdf:Property . "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Literal . }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Resource . }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range ?rangeClass . ?rangeClass a ?rangeClassType . }"
                + "BIND(rdf:Property as ?type)"
                + "} "
                + "FILTER(STRSTARTS(STR(?predicate), STR(?externalModel)))"
                + "OPTIONAL { ?predicate rdfs:label ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) }"
                + "OPTIONAL { ?predicate rdfs:label ?label . FILTER(LANG(?label)!='') }"
                + "} "
                + "}";
        
        pss.setIri("library", model);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
         
        pss.setCommandText(queryString);

        return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getImportsSparqlAddress());

      } else {
          
            try {
                idIRI = iriFactory.construct(id);
            }
            catch (IRIException e) {
                return JerseyResponseManager.invalidIRI();
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
                + "} WHERE { "
                 + "SERVICE ?modelService { "
                     + "GRAPH ?library { "
                     + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                     + "}}"
                 + "GRAPH ?externalModel { "
               + "{"
                + "?predicate a owl:DatatypeProperty . "
                + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
                + "BIND(owl:DatatypeProperty as ?type) "
                + "} UNION {"
                + "?predicate a owl:ObjectProperty . "
                + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                + "BIND(owl:ObjectProperty as ?type) "
                + "} UNION {"
                /* Treat owl:AnnotationProperty as DatatypeProperty */
                + "?predicate a owl:AnnotationProperty. "
                + "?predicate rdfs:label ?atLeastSomeLabel . "
                + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                + "BIND(owl:DatatypeProperty as ?type) "
                + "} UNION {"
                /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
                + "?predicate a rdf:Property . "
                + "?predicate rdfs:range rdfs:Literal ."
                + "BIND(owl:DatatypeProperty as ?type) "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
                 + "} UNION {"
                /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
                + "?predicate a rdf:Property . "
                + "?predicate rdfs:range rdfs:Resource ."
                + "BIND(owl:ObjectProperty as ?type) "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty  } }"
                + "} UNION {"
                /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
                + "?predicate a rdf:Property . "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
                + "?predicate rdfs:range ?rangeClass . "
                + "FILTER(?rangeClass!=rdfs:Literal)"
                + "?rangeClass a ?rangeClassType . "
                + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
                + "BIND(owl:ObjectProperty as ?type) "
                + "} UNION {"
                /* IF Predicate type cannot be guessed */
                + "?predicate a rdf:Property . "
                + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Literal . }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Resource . }"
                + "FILTER NOT EXISTS { ?predicate rdfs:range ?rangeClass . ?rangeClass a ?rangeClassType . }"
                + "BIND(rdf:Property as ?type)"
                + "} "
                + "OPTIONAL { ?predicate rdfs:range ?range . FILTER (!isBlank(?range)) }"
                /* TODO: Support only direct range no OWL unions */
                //   + "OPTIONAL { ?predicate rdfs:domain ?domain .  }"
                + "OPTIONAL { ?predicate rdfs:label ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(STR(?labelStr),'en') as ?label) }"
                + "OPTIONAL { ?predicate rdfs:label ?label . FILTER(LANG(?label)!='') }"
                + "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                   
                      /* Predicate comments - if lang unknown create english tag */
                    + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyCommentStr . FILTER(LANG(?propertyCommentStr) = '') "
                    + "BIND(STRLANG(STR(?propertyCommentStr),'en') as ?comment) }"
                    
                    + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyCommentToStr . FILTER(LANG(?propertyCommentToStr)!='') "
                    + "BIND(STR(?propertyCommentToStr) as ?comment) }"

                + "} "
                + "}";
        
            pss.setCommandText(queryString);

            pss.setIri("predicate", idIRI);   
            pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
         

            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }
            
            
            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), sparqlService);         

      }
         
  }

}
