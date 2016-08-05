/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.profile;

import com.csc.fi.ioapi.config.ApplicationProperties;
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
import com.csc.fi.ioapi.utils.ConceptMapper;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
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
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    classIRI = iri.construct(classID);
                    profileIRI = iri.construct(profileID);
                    shapeIRI = iri.construct(profileIRI+"#"+SplitIRI.localname(classID));
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
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
                    + "?shapeIRI sh:property ?property . "
                    + "?shapeIRI dcterms:subject ?subject . "
                    + "?subject ?sp ?so . "
                    + "?property ?p ?o . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                   // + "BIND(iri(concat(?profileNamespace,concat('s',afn:localname(?classIRI)))) as ?shapeIRI)"   
                    + "GRAPH ?classIRI { "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI rdfs:label ?label . "
                    + "OPTIONAL { ?classIRI rdfs:comment ?comment . } "
                    + "OPTIONAL { ?classIRI dcterms:subject ?subject . "
                    + " ?subject ?sp ?so . }"
                    + "OPTIONAL { "
                    + "?classIRI sh:property ?property . "
                    + "?property ?p ?o . } "
                    + "}}";

            } else {           
            /* Create Shape from external IMPORT */   
                service = services.getImportsSparqlAddress();
                queryString = "CONSTRUCT  { "
                    + "?shapeIRI owl:versionInfo ?draft . "
                    + "?shapeIRI dcterms:modified ?modified . "
                    + "?shapeIRI dcterms:created ?creation . "
                    + "?shapeIRI sh:scopeClass ?classIRI . "
                    + "?shapeIRI a sh:Shape . "
                    + "?shapeIRI rdfs:isDefinedBy ?model . "
                    + "?shapeIRI rdfs:label ?label . "
                    + "?shapeIRI rdfs:comment ?comment . "
                    + "?shapeIRI sh:property ?property . "
                    + "?property dcterms:type ?predicateType . "    
                    + "?property sh:predicate ?predicate . "
                    + "?property rdfs:comment ?propertyComment .  "
                    + "?property rdfs:label ?propertyLabel .  "
                    + "?property sh:valueShape ?valueClass . "
                    + "?property sh:datatype ?prefDatatype . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "OPTIONAL { ?classIRI a ?type . "
                    + "VALUES ?type { owl:Class rdfs:Class }}"
                        
                    + "OPTIONAL {?classIRI rdfs:label ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) }"
                    + "OPTIONAL {?classIRI rdfs:label ?label . FILTER(LANG(?label)!='') }"
                        
                    + "OPTIONAL { ?classIRI rdfs:comment ?commentStr . "
                    + "BIND(STRLANG(?commentStr,'en') as ?comment) "
                    + "}"
                    + "OPTIONAL { "
                    + "?classIRI rdfs:subClassOf* ?superclass . "
                    + "?predicate rdfs:domain ?superclass .  "
                    + "?predicate a ?predicateType . "
                    + "VALUES ?predicateType { owl:DatatypeProperty owl:ObjectProperty }"
                    + "BIND(UUID() AS ?property)"    
                    + "OPTIONAL { ?predicate a owl:DatatypeProperty . "
                    + "?predicate rdfs:range ?datatype . "
                    + "BIND(IF(?datatype=rdfs:Literal,xsd:string,?datatype) as ?prefDatatype) } "
                    + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . } "
                        
                    /* Predicate label - if lang unknown create english tag */
                    + "OPTIONAL {?predicate rdfs:label ?propertyLabelStr . FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
                    + "OPTIONAL { ?predicate rdfs:label ?propertyLabel . FILTER(LANG(?propertyLabel)!='') }"
                    
                    /* Predicate comments - if lang unknown create english tag */
                    + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyCommentStr . "
                    + "VALUES ?predicateCommentPred { rdfs:comment skos:definition prov:definition dcterms:description dc:description }"
                    + "FILTER(LANG(?propertyCommentStr) = '') BIND(STRLANG(?propertyCommentStr,'en') as ?propertyComment) }"
                    + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyComment . "
                    + "VALUES ?predicateCommentPred { rdfs:comment skos:definition prov:definition dcterms:description dc:description }"
                    + " FILTER(LANG(?propertyComment)!='') }"
                        
                    + "}"    
                    + "}";
                               
            }
   
            pss.setCommandText(queryString);
            pss.setIri("classIRI", classIRI);
            pss.setIri("model", profileIRI);
            pss.setLiteral("profileNamespace", profileID+"#");
            pss.setLiteral("draft", "Unstable");
            pss.setIri("shapeIRI",shapeIRI);


            return JerseyFusekiClient.constructGraphFromService(pss.toString(), service);
    }   
 
}
