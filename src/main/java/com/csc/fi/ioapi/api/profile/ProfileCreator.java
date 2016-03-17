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
@Path("profileCreator")
@Api(value = "/profileCreator", description = "Construct new profile template")
public class ProfileCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ProfileCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new profile", notes = "Create new profile")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New profile is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newPfofile(
            @ApiParam(value = "Profile prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Profile label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Group ID", required = true) @QueryParam("group") String group,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

            prefix = LDHelper.modelName(prefix);
            String namespace = ApplicationProperties.getProfileNamespace()+prefix;
            
            IRI groupIRI,namespaceIRI,namespaceSKOSIRI;
            
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    namespaceIRI = iri.construct(namespace);
                    namespaceSKOSIRI = iri.construct(namespace+"/skos#");
                    groupIRI = iri.construct(group);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }

            
            if(GraphManager.isExistingPrefix(prefix)) {
                return Response.status(405).entity(ErrorMessage.USEDIRI).build();
            }
            
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            pss.setNsPrefix(prefix, namespace+"#");
            
            String queryString = "CONSTRUCT  { "
                    + "?modelIRI a owl:Ontology . "
                    + "?modelIRI a dcap:DCAP . "
                    + "?modelIRI rdfs:label ?profileLabel . "
                    + "?modelIRI owl:versionInfo ?draft . "
                    + "?modelIRI dcterms:created ?creation . "
                    + "?modelIRI dcterms:modified ?creation . "
                    + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                    + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?modelIRI dcterms:isPartOf ?group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "?modelIRI dcterms:references ?localSKOSNamespace . "
                    + "?localSKOSNamespace a skos:Collection ; "
                    + " dcterms:identifier ?prefix ; "
                    + " dcterms:title ?profileLabelSKOS . "
                    + "?modelIRI dcterms:references <http://jhsmeta.fi/skos/> . "
                    + "<http://jhsmeta.fi/skos/> a skos:ConceptScheme ; "
                    + " dcterms:identifier 'jhsmeta' ; "
                    + " dcterms:title 'JHSmeta'@fi . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "GRAPH <urn:csc:groups> { "
                    + "?group a foaf:Group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "}"
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("localSKOSNamespace", namespaceSKOSIRI);
            pss.setLiteral("profileLabelSKOS", ResourceFactory.createLangLiteral("Sisäinen käsitteistö", lang));
            pss.setLiteral("namespace", namespace+"#");
            pss.setLiteral("prefix", prefix);
            pss.setIri("modelIRI", namespaceIRI);
            pss.setIri("group", groupIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("profileLabel", ResourceFactory.createLangLiteral(label, lang));

            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
            
    }   
 
}
