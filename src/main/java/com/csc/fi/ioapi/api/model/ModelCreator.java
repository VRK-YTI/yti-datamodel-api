/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "modelCreator" path)
 */
@Path("modelCreator")
@Api(value = "/modelCreator", description = "Construct new model template")
public class ModelCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ModelCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new model", notes = "Create new model")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newModel(
            @ApiParam(value = "Model prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Model label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Group ID", required = true) @QueryParam("group") String group,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

            prefix = LDHelper.modelName(prefix);
            String namespace = ApplicationProperties.getDefaultNamespace()+prefix;
            
            IRI groupIRI, namespaceIRI;
            
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    namespaceIRI = iri.construct(namespace);
                    groupIRI = iri.construct(group);
            } catch (IRIException e) {
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }
            
        if(GraphManager.isExistingGraph(namespaceIRI)) {
            return Response.status(405).entity(ErrorMessage.USEDIRI).build();
        }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            pss.setNsPrefix(prefix, namespace+"#");
            
            String queryString = "CONSTRUCT  { "
                    + "?modelIRI a owl:Ontology . "
                    + "?modelIRI rdfs:label ?mlabel . "
                    + "?modelIRI owl:versionInfo ?draft . "
                    + "?modelIRI dcterms:created ?creation . "
                    + "?modelIRI dcterms:modified ?creation . "
                    + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                    + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?modelIRI dcterms:isPartOf ?group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "GRAPH <urn:csc:groups> { "
                    + "?group a foaf:Group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "}"
                    + "}";

            pss.setCommandText(queryString);
            pss.setLiteral("namespace", namespace+"#");
            pss.setLiteral("prefix", prefix);
            pss.setIri("modelIRI", namespaceIRI);
            pss.setIri("group", groupIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("mlabel", ResourceFactory.createLangLiteral(label, lang));

            logger.info(pss.toString());
            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
            
    }   
 
}
