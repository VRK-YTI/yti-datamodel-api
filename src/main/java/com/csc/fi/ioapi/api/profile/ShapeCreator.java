/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.profile;

import java.util.logging.Level;
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
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.QueryLibrary;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.util.SplitIRI;

/**
 * Root resource (exposed at "classCreator" path)
 */
@Path("shapeCreator")
@Api(value = "/shapeCreator", description = "Construct new Shape template")
public class ShapeCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ShapeCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Profile ID", required = true) @QueryParam("profileID") String profileID,
            @ApiParam(value = "Class ID", required = true) @QueryParam("classID") String classID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

            IRI classIRI,profileIRI,shapeIRI;
            try {
                    classIRI = IDManager.constructIRI(classID);
                    profileIRI = IDManager.constructIRI(profileID);
                    if(profileID.endsWith("/") || profileID.endsWith("#")) {
                       shapeIRI = IDManager.constructIRI(profileIRI+SplitIRI.localname(classID)); 
                    } else {
                        shapeIRI = IDManager.constructIRI(profileIRI+"#"+SplitIRI.localname(classID));
                    }
                    
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return JerseyResponseManager.invalidIRI();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            String queryString;
            String service;
                
            /* Create Shape from Class */
           if(GraphManager.isExistingServiceGraph(SplitIRI.namespace(classID))) {

                service = services.getCoreSparqlAddress();
                queryString = "CONSTRUCT  { "
                    + "?shapeIRI owl:versionInfo ?draft . "
                    + "?shapeIRI dcterms:modified ?modified . "
                    + "?shapeIRI dcterms:created ?creation . "
                    + "?shapeIRI sh:scopeClass ?classIRI . "
                    + "?shapeIRI a sh:Shape . "
                    + "?shapeIRI rdfs:isDefinedBy ?model . "
                    + "?shapeIRI rdfs:label ?label . "
                    + "?shapeIRI rdfs:comment ?comment . "
                    + "?shapeIRI dcterms:subject ?concept . "
                    + "?concept skos:inScheme ?scheme . "
                    + "?scheme dcterms:title ?title . "
                    + "?scheme termed:id ?schemeId . "
                    + "?scheme termed:graph ?termedGraph . "
                    + "?concept termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "?shapeIRI sh:property ?shapeuuid . "
                    + "?shapeuuid ?p ?o . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "GRAPH ?classIRI { "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI rdfs:label ?label . "
                    + "OPTIONAL { ?classIRI rdfs:comment ?comment . } "
                        
                    + "{ "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:inScheme ?collection . "
                    + "} UNION { "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:inScheme ?collection . "
                    + "?collection dcterms:title ?title . "
                    + "} UNION { "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:inScheme ?scheme . "
                    + "?scheme dcterms:title ?title . "
                    + "?scheme termed:id ?schemeId . "
                    + "?scheme termed:graph ?termedGraph . "
                    + "?concept termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "}"
                        
                    + "OPTIONAL {"
                    + "?classIRI sh:property ?property .  "
                    /* Todo: Issue 472 */
                   + "BIND(IRI(CONCAT(STR(?property),?shapePropertyID)) as ?shapeuuid)" 
                   + "?property ?p ?o . "
                    + "}} "
                    + "}";
                
                pss.setLiteral("shapePropertyID", "-"+UUID.randomUUID().toString());

            } else {           
            /* Create Shape from external IMPORT */   
                service = services.getImportsSparqlAddress();
                logger.info("Using ext query:");
                queryString = QueryLibrary.externalShapeQuery;
                logger.info(queryString);           
            }
   
            pss.setCommandText(queryString);
            pss.setIri("classIRI", classIRI);
            pss.setIri("model", profileIRI);
            pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
            pss.setLiteral("draft", "Unstable");
            pss.setIri("shapeIRI",shapeIRI);


            return JerseyJsonLDClient.constructNotEmptyGraphFromService(pss.toString(), service);
    }   
 
}
