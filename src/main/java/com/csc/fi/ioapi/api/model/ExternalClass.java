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
@Path("externalClass")
@Api(value = "/externalClass", description = "External class operations")
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
                + "?class rdfs:label ?label . "
                + "?class a rdfs:Class . "
                + "?class dcterms:modified ?modified . "
                + "?class rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabel . "
                + "} WHERE { "
                 + "SERVICE ?modelService { "
                 + "GRAPH ?library { "
                 + "?library dcterms:requires ?externalModel . "
                 + "}}"
                 + "GRAPH ?externalModel { "
                 + "?class a ?type . "
                 + "VALUES ?type { rdfs:Class owl:Class sh:Shape } "
                 + "?class rdfs:label ?labelStr . BIND(STRLANG(?labelStr,'en') as ?label) "
                 + "OPTIONAL { ?class rdfs:isDefinedBy ?source . ?source rdfs:label ?sourceLabelStr .  BIND(STRLANG(?sourceLabelStr,'en') as ?sourceLabel)} "
                 + "} "
                 + "}";
        

        pss.setIri("library", model);
        pss.setIri("modelService",services.getCoreSparqlAddress());
         
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
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI rdfs:label ?label . "
                    + "?classIRI rdfs:comment ?comment . "
                    + "?classIRI sh:property ?property . "
                    + "?property sh:datatype ?datatype . "
                    + "?property sh:valueClass ?valueClass . "
                    + "?property sh:predicate ?predicate . "
                    + "?property rdfs:label ?propertyLabel . "
                    + "?property rdfs:comment ?propertyComment . "
                     + "} WHERE { "
                    + "?classIRI a ?type . "
                    + "OPTIONAL { ?classIRI rdfs:label ?labelStr . BIND(STRLANG(?labelStr,'en') as ?label) }"
                    + "OPTIONAL { ?classIRI rdfs:comment ?commentStr . BIND(STRLANG(?commentStr,'en') as ?comment)}"
                    + "OPTIONAL { "
                    + "?predicate rdfs:domain ?classIRI .  "
                    + "BIND(UUID() AS ?property)"    
                    + "OPTIONAL { ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . } "
                    + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . } "
                    + "OPTIONAL { ?predicate rdfs:label ?propertyLabelStr . BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
                    + "OPTIONAL { ?predicate rdfs:comment ?propertyCommentStr . BIND(STRLANG(?propertyCommentStr,'en') as ?propertyComment) }"
                    + "} }";
            
            pss.setCommandText(queryString);

            pss.setIri("classIRI", idIRI);

            if(model!=null && !model.equals("undefined")) {
                  pss.setIri("library", model);
            }
                        return JerseyFusekiClient.constructGraphFromService(pss.toString(), sparqlService);         

      }
         
  }

}
