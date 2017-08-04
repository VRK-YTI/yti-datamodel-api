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
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

/**
 * Root resource (exposed at "classCreator" path)
 */
@Path("classCreator")
@Api(tags = {"Class"}, description = "Construct new Class template")
public class ClassCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ClassCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Class label", required = true) @QueryParam("classLabel") String classLabel,
            @ApiParam(value = "Concept ID", required = true) @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

            IRI conceptIRI,modelIRI;
            try {
                    conceptIRI = IDManager.constructIRI(conceptID);
                    modelIRI = IDManager.constructIRI(modelID);
            } catch (IRIException e) {
                    return JerseyResponseManager.invalidIRI();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            String queryString = "CONSTRUCT  { "
                    + "?classIRI iow:contextIdentifier ?localIdentifier . "
                    + "?classIRI owl:versionInfo ?draft . "
                    + "?classIRI dcterms:modified ?modified . "
                    + "?classIRI dcterms:created ?creation . "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI rdfs:isDefinedBy ?model . "
                    + "?model rdfs:label ?modelLabel . "
                    + "?model a ?modelType . "
                    + "?classIRI rdfs:label ?classLabel . "
                    + "?classIRI rdfs:comment ?comment . "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept a skos:Concept . "
                    + "?concept skos:prefLabel ?label . "
                    + "?concept skos:definition ?comment . "
                    + "?concept skos:inScheme ?scheme . "
                    + "?scheme dcterms:title ?title . "
                    + "?scheme termed:id ?schemeId . "
                    + "?scheme termed:graph ?termedGraph . "
                    + "?concept termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "?model a ?modelType . "
                    + "?model rdfs:label ?modelLabel . "
                    + "?concept a skos:Concept . "
                    + "?concept skos:inScheme ?scheme . "
                    + "?scheme termed:id ?schemeId . "
                    + "?concept skos:prefLabel ?label . "
                    + "?concept termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "?scheme dcterms:title ?title . "
                    + "?scheme termed:graph ?termedGraph . "
                    + "OPTIONAL {"
                    + "?concept skos:definition ?comment . } "
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("concept", conceptIRI);
            pss.setIri("model", modelIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("classLabel", ResourceFactory.createLangLiteral(classLabel, lang));
            String resourceName = LDHelper.resourceName(classLabel);
            pss.setIri("classIRI",LDHelper.resourceIRI(modelID,resourceName));

            logger.info("New classCreator template from "+conceptID);
           
            return JerseyJsonLDClient.constructFromTermedAndCore(conceptIRI.toString(), modelIRI.toString(), pss.asQuery());
            
    }   
 
}
